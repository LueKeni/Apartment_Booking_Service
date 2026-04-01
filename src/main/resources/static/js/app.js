function normalizeText(value) {
  return (value || "").toString().trim().toLowerCase();
}

function parseSortValue(raw) {
  const value = (raw || "").toString().trim();
  if (value === "") {
    return "";
  }
  const numeric = Number(value);
  if (!Number.isNaN(numeric) && value !== "") {
    return numeric;
  }
  const parsedDate = Date.parse(value);
  if (!Number.isNaN(parsedDate)) {
    return parsedDate;
  }
  return value.toLowerCase();
}

function initRealtimeTable(tableRoot) {
  const body = tableRoot.querySelector("[data-table-body]");
  if (!body) {
    return;
  }

  const rows = Array.from(body.querySelectorAll("[data-row]"));
  const searchInput = tableRoot.querySelector("[data-table-search]");
  const sortSelect = tableRoot.querySelector("[data-table-sort]");
  const filters = Array.from(tableRoot.querySelectorAll("[data-table-filter-key]"));
  const emptyState = tableRoot.querySelector("[data-table-empty]");

  const apply = () => {
    const query = normalizeText(searchInput ? searchInput.value : "");
    const sortSpec = sortSelect ? sortSelect.value : "";
    const hasActiveFilters =
      query.length > 0 ||
      !!sortSpec ||
      filters.some((filter) => normalizeText(filter.value).length > 0);

    let filtered = rows.filter((row) => {
      const searchHaystack = normalizeText(row.dataset.search);
      if (query && !searchHaystack.includes(query)) {
        return false;
      }

      for (const filter of filters) {
        const key = filter.dataset.tableFilterKey;
        const selected = normalizeText(filter.value);
        if (!key || selected === "") {
          continue;
        }
        const rowValue = normalizeText(row.dataset[key]);
        if (rowValue !== selected) {
          return false;
        }
      }
      return true;
    });

    if (sortSpec) {
      const [key, direction] = sortSpec.split(":");
      filtered = filtered.slice().sort((a, b) => {
        const left = parseSortValue(a.dataset[key]);
        const right = parseSortValue(b.dataset[key]);

        if (left < right) {
          return direction === "desc" ? 1 : -1;
        }
        if (left > right) {
          return direction === "desc" ? -1 : 1;
        }
        return 0;
      });
    }

    rows.forEach((row) => {
      row.classList.add("hidden");
    });
    filtered.forEach((row) => {
      row.classList.remove("hidden");
      body.appendChild(row);
    });

    if (emptyState) {
      const shouldShowEmpty = rows.length > 0 && filtered.length === 0 && hasActiveFilters;
      emptyState.classList.toggle("hidden", !shouldShowEmpty);
    }
  };

  if (searchInput) {
    searchInput.addEventListener("input", apply);
  }
  if (sortSelect) {
    sortSelect.addEventListener("change", apply);
  }
  filters.forEach((filter) => {
    filter.addEventListener("change", apply);
  });

  apply();
}

function initChat(root) {
  const conversationList = root.querySelector("[data-chat-conversation-list]");
  const emptyState = root.querySelector("[data-chat-empty]");
  const thread = root.querySelector("[data-chat-thread]");
  const placeholder = root.querySelector("[data-chat-placeholder]");
  const title = root.querySelector("[data-chat-title]");
  const subtitle = root.querySelector("[data-chat-subtitle]");
  const status = root.querySelector("[data-chat-status]");
  const form = root.querySelector("[data-chat-form]");
  const input = root.querySelector("[data-chat-input]");
  const sendButton = root.querySelector("[data-chat-send]");
  const refreshButton = root.querySelector("[data-chat-refresh]");

  if (!conversationList || !thread || !form || !input || !sendButton) {
    return;
  }

  const toNumber = (value) => {
    if (value === null || value === undefined || value === "") {
      return null;
    }
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  };

  const state = {
    currentUserId: toNumber(root.dataset.currentUserId),
    conversationId: toNumber(root.dataset.initialConversationId),
    recipientId: toNumber(root.dataset.initialRecipientId),
    apartmentId: toNumber(root.dataset.initialApartmentId)
  };

  let activeConversation = null;
  let cachedConversations = [];
  let pollingId = null;

  const formatTimestamp = (value) => {
    if (!value) {
      return "";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleString(undefined, {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    });
  };

  const setStatus = (text) => {
    if (status) {
      status.textContent = text;
    }
  };

  const setComposerEnabled = (enabled) => {
    input.disabled = !enabled;
    sendButton.disabled = !enabled;
    sendButton.classList.toggle("opacity-50", !enabled);
    sendButton.classList.toggle("cursor-not-allowed", !enabled);
  };

  const updateHeader = () => {
    if (activeConversation) {
      title.textContent = activeConversation.otherUserName || "Conversation";
      subtitle.textContent = activeConversation.apartmentTitle
        ? `Listing: ${activeConversation.apartmentTitle}`
        : "General Inquiry";
      return;
    }
    if (state.recipientId) {
      title.textContent = "New conversation";
      subtitle.textContent = "Send your first message to start this chat.";
      return;
    }
    title.textContent = "Select a conversation";
    subtitle.textContent = "Choose a listing to view messages.";
  };

  const renderMessages = (messages) => {
    thread.innerHTML = "";
    messages.forEach((message) => {
      const isSelf = state.currentUserId && message.senderId === state.currentUserId;
      const wrapper = document.createElement("div");
      wrapper.className = `flex ${isSelf ? "justify-end" : "justify-start"}`;

      const bubble = document.createElement("div");
      bubble.className = `max-w-[80%] rounded-2xl px-4 py-2 text-sm shadow-sm ${
        isSelf ? "bg-ink text-white" : "bg-white text-slate-700"
      }`;

      const content = document.createElement("p");
      content.textContent = message.content;

      const meta = document.createElement("p");
      meta.className = `mt-1 text-xs ${isSelf ? "text-white/70" : "text-slate-400"}`;
      meta.textContent = formatTimestamp(message.sentAt);

      bubble.appendChild(content);
      bubble.appendChild(meta);
      wrapper.appendChild(bubble);
      thread.appendChild(wrapper);
    });

    if (placeholder) {
      placeholder.classList.toggle("hidden", messages.length > 0);
    }
    thread.scrollTop = thread.scrollHeight;
  };

  const renderConversationList = () => {
    conversationList.innerHTML = "";

    if (!cachedConversations.length) {
      emptyState?.classList.remove("hidden");
      return;
    }

    emptyState?.classList.add("hidden");
    cachedConversations.forEach((conversation) => {
      const button = document.createElement("button");
      button.type = "button";
      const isActive = state.conversationId === conversation.conversationId;
      button.className = `w-full rounded-xl border px-3 py-3 text-left transition ${
        isActive
          ? "border-transparent bg-ink text-white shadow"
          : "border-slate-200 bg-white/70 text-slate-700 hover:bg-white"
      }`;

      const name = document.createElement("p");
      name.className = "text-sm font-semibold";
      name.textContent = conversation.otherUserName || "Conversation";

      const listing = document.createElement("p");
      listing.className = `mt-1 text-xs ${isActive ? "text-white/70" : "text-slate-500"}`;
      listing.textContent = conversation.apartmentTitle || "General Inquiry";

      const meta = document.createElement("p");
      meta.className = `mt-2 text-xs ${isActive ? "text-white/60" : "text-slate-400"}`;
      meta.textContent = conversation.lastMessageAt ? formatTimestamp(conversation.lastMessageAt) : "";

      button.appendChild(name);
      button.appendChild(listing);
      button.appendChild(meta);

      button.addEventListener("click", () => {
        selectConversation(conversation);
      });

      conversationList.appendChild(button);
    });
  };

  const selectConversation = (conversation) => {
    activeConversation = conversation;
    state.conversationId = conversation.conversationId;
    state.recipientId = conversation.otherUserId;
    state.apartmentId = conversation.apartmentId || null;
    updateHeader();
    setComposerEnabled(true);
    loadMessages();
    renderConversationList();
  };

  const loadMessages = async (silent = false) => {
    if (!state.conversationId) {
      thread.innerHTML = "";
      if (placeholder) {
        placeholder.classList.remove("hidden");
      }
      setComposerEnabled(Boolean(state.recipientId));
      updateHeader();
      return;
    }

    if (!silent) {
      setStatus("Loading...");
    }

    try {
      const response = await fetch(`/api/chat/conversations/${state.conversationId}/messages`);
      if (!response.ok) {
        throw new Error("Failed to load messages");
      }
      const messages = await response.json();
      renderMessages(messages);
      await fetch(`/api/chat/conversations/${state.conversationId}/read`, { method: "POST" });
      setStatus("Ready");
    } catch (error) {
      setStatus("Unable to load messages");
    }
  };

  const refreshConversations = async () => {
    setStatus("Syncing...");
    try {
      const response = await fetch("/api/chat/conversations");
      if (!response.ok) {
        throw new Error("Failed to load conversations");
      }
      cachedConversations = await response.json();
      renderConversationList();

      if (state.conversationId) {
        const matched = cachedConversations.find((item) => item.conversationId === state.conversationId);
        activeConversation = matched || null;
      } else if (state.recipientId) {
        const matched = cachedConversations.find(
          (item) => item.otherUserId === state.recipientId && (state.apartmentId ? item.apartmentId === state.apartmentId : true)
        );
        if (matched) {
          selectConversation(matched);
          return;
        }
      }

      updateHeader();
      setComposerEnabled(Boolean(state.recipientId));
      setStatus("Ready");
      if (state.conversationId) {
        loadMessages(true);
      }
    } catch (error) {
      setStatus("Unable to sync");
    }
  };

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const content = input.value.trim();
    if (!content) {
      return;
    }
    if (!state.recipientId) {
      setStatus("Select a conversation first");
      return;
    }

    setStatus("Sending...");
    try {
      const response = await fetch("/api/chat/messages", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          conversationId: state.conversationId,
          recipientId: state.recipientId,
          apartmentId: state.apartmentId,
          content
        })
      });

      if (!response.ok) {
        throw new Error("Failed to send message");
      }

      const saved = await response.json();
      state.conversationId = saved.conversationId;
      input.value = "";
      await refreshConversations();
      await loadMessages(true);
      setStatus("Sent");
    } catch (error) {
      setStatus("Send failed");
    }
  });

  if (refreshButton) {
    refreshButton.addEventListener("click", refreshConversations);
  }

  refreshConversations();

  if (pollingId) {
    clearInterval(pollingId);
  }
  pollingId = setInterval(() => {
    if (state.conversationId) {
      loadMessages(true);
    }
  }, 7000);
}

function initChatUnreadBadge() {
  const badges = Array.from(document.querySelectorAll("[data-chat-unread-badge]"));
  if (!badges.length) {
    return;
  }

  const updateBadges = async () => {
    try {
      const response = await fetch("/api/chat/unread-count");
      if (!response.ok) {
        throw new Error("Failed to load unread count");
      }
      const count = Number(await response.json());
      const display = Number.isFinite(count) && count > 99 ? "99+" : String(count || 0);
      badges.forEach((badge) => {
        if (count && count > 0) {
          badge.textContent = display;
          badge.classList.remove("hidden");
        } else {
          badge.classList.add("hidden");
        }
      });
    } catch (error) {
      badges.forEach((badge) => badge.classList.add("hidden"));
    }
  };

  updateBadges();
  setInterval(updateBadges, 10000);
  document.addEventListener("visibilitychange", () => {
    if (!document.hidden) {
      updateBadges();
    }
  });
}

function initComparison() {
  const bar = document.getElementById("comparison-bar");
  const countLabel = document.getElementById("compare-count");
  const itemsContainer = document.getElementById("compare-items");
  const clearBtn = document.getElementById("clear-compare");
  const compareBtn = document.getElementById("compare-now-btn");
  const modal = document.getElementById("comparison-modal");
  const modalContent = document.getElementById("comparison-content");
  const closeModalBtn = document.getElementById("close-comparison-modal");

  if (!bar || !countLabel || !itemsContainer || !clearBtn || !compareBtn || !modal) {
    return;
  }

  let compareList = [];

  const updateUI = () => {
    const count = compareList.length;
    countLabel.textContent = `${count}/3`;

    if (count > 0) {
      bar.classList.remove("translate-y-full");
    } else {
      bar.classList.add("translate-y-full");
    }

    compareBtn.disabled = count < 2;

    itemsContainer.innerHTML = "";
    compareList.forEach((item) => {
      const div = document.createElement("div");
      div.className = "relative group w-12 h-12";
      div.innerHTML = `
        <img src="${item.image}" class="w-12 h-12 rounded-lg object-cover border-2 border-white shadow-sm" alt="${item.title}" />
        <button type="button" class="absolute -top-1 -right-1 bg-coral text-white rounded-full w-4 h-4 text-[10px] flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity" data-remove="${item.id}">
          <i class="fas fa-times"></i>
        </button>
      `;
      div.querySelector("[data-remove]").addEventListener("click", () => {
        removeFromCompare(item.id);
      });
      itemsContainer.appendChild(div);
    });
  };

  const addToCompare = (id, title, price, image, district, type, area) => {
    if (compareList.length >= 3) {
      alert("You can only compare up to 3 apartments.");
      return;
    }
    if (compareList.some((item) => item.id === id)) {
      alert("This apartment is already in the comparison list.");
      return;
    }

    compareList.push({ id, title, price, image, district, type, area });
    updateUI();
  };

  const removeFromCompare = (id) => {
    compareList = compareList.filter((item) => item.id !== id);
    updateUI();
  };

  const renderComparisonTable = () => {
    if (compareList.length < 2) return;

    let html = `
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse min-w-[600px]">
          <thead>
            <tr>
              <th class="p-3 bg-slate-50 border border-slate-200 w-1/4">Feature</th>
              ${compareList.map(item => `
                <th class="p-3 bg-slate-50 border border-slate-200">
                  <div class="flex flex-col items-center text-center">
                    <img src="${item.image}" class="w-32 h-20 rounded-lg object-cover mb-2" />
                    <p class="font-bold text-ink">${item.title}</p>
                    <p class="text-tealdeep font-bold mt-1">${item.price}</p>
                  </div>
                </th>
              `).join('')}
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="p-3 border border-slate-200 font-semibold bg-slate-50">District</td>
              ${compareList.map(item => `<td class="p-3 border border-slate-200 text-center">${item.district || "N/A"}</td>`).join('')}
            </tr>
            <tr>
              <td class="p-3 border border-slate-200 font-semibold bg-slate-50">Room Type</td>
              ${compareList.map(item => `<td class="p-3 border border-slate-200 text-center">${item.type || "N/A"}</td>`).join('')}
            </tr>
            <tr>
              <td class="p-3 border border-slate-200 font-semibold bg-slate-50">Area</td>
              ${compareList.map(item => `<td class="p-3 border border-slate-200 text-center">${item.area || "N/A"} m²</td>`).join('')}
            </tr>
            <tr>
              <td class="p-3 border border-slate-200 font-semibold bg-slate-50">Action</td>
              ${compareList.map(item => `
                <td class="p-3 border border-slate-200 text-center">
                  <a href="/apartments/${item.id}" class="inline-block rounded-lg bg-ink px-4 py-2 text-xs font-semibold text-white">View Details</a>
                </td>
              `).join('')}
            </tr>
          </tbody>
        </table>
      </div>
    `;
    modalContent.innerHTML = html;
  };

  document.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-compare-add]");
    if (btn) {
      const data = btn.dataset;
      addToCompare(
        data.compareAdd,
        data.compareTitle,
        data.comparePrice,
        data.compareImage,
        data.compareDistrict,
        data.compareType,
        data.compareArea
      );
    }
  });

  clearBtn.addEventListener("click", () => {
    compareList = [];
    updateUI();
  });

  compareBtn.addEventListener("click", () => {
    renderComparisonTable();
    modal.classList.remove("hidden");
  });

  closeModalBtn.addEventListener("click", () => {
    modal.classList.add("hidden");
  });

  modal.addEventListener("click", (e) => {
    if (e.target === modal) {
      modal.classList.add("hidden");
    }
  });
}

function initApartmentAssistant(root) {
  const toggle = root.querySelector("[data-assistant-toggle]");
  const panel = root.querySelector("[data-assistant-panel]") || root.querySelector("#assistant-panel");
  const form = root.querySelector("[data-assistant-form]");
  const input = root.querySelector("[data-assistant-input]");
  const submit = root.querySelector("[data-assistant-submit]");
  const thread = root.querySelector("[data-assistant-thread]");
  const status = root.querySelector("[data-assistant-status]");
  const prompts = root.querySelectorAll("[data-assistant-prompt]");

  if (!form || !input || !submit || !thread || !status) {
    return;
  }

  const setOpen = (open) => {
    if (!panel || !toggle) {
      return;
    }
    panel.classList.toggle("hidden", !open);
    toggle.setAttribute("aria-expanded", String(open));
  };

  const setStatus = (text) => {
    status.textContent = text;
  };

  const scrollThreadToBottom = () => {
    thread.scrollTop = thread.scrollHeight;
  };

  const appendMessage = (text, sender, suggestions = [], appliedFilters = []) => {
    const row = document.createElement("div");
    row.className = `assistant-row ${sender === "user" ? "assistant-row-user" : "assistant-row-bot"}`;

    const bubble = document.createElement("div");
    bubble.className = `assistant-bubble ${sender === "user" ? "assistant-bubble-user" : "assistant-bubble-bot"}`;
    bubble.textContent = text;
    row.appendChild(bubble);

    if (sender === "bot" && appliedFilters.length) {
      const filterWrap = document.createElement("div");
      filterWrap.className = "assistant-filter-wrap";
      appliedFilters.forEach((item) => {
        const chip = document.createElement("span");
        chip.className = "assistant-filter-chip";
        chip.textContent = item;
        filterWrap.appendChild(chip);
      });
      row.appendChild(filterWrap);
    }

    if (sender === "bot" && suggestions.length) {
      const cards = document.createElement("div");
      cards.className = "assistant-cards";
      suggestions.forEach((item) => {
        const card = document.createElement("a");
        card.href = item.detailUrl;
        card.className = "assistant-card";
        card.innerHTML = `
          <div class="assistant-card-image-wrap">
            ${item.imageUrl
              ? `<img src="${item.imageUrl}" alt="${item.title}" class="assistant-card-image" />`
              : `<div class="assistant-card-fallback">No image</div>`}
          </div>
          <div class="assistant-card-body">
            <p class="assistant-card-title">${item.title || "Apartment"}</p>
            <p class="assistant-card-meta">${item.district || "Unknown district"} • ${item.roomType || "N/A"}</p>
            <p class="assistant-card-price">${item.priceLabel || "Lien he"}</p>
          </div>
        `;
        cards.appendChild(card);
      });
      row.appendChild(cards);
    }

    thread.appendChild(row);
    scrollThreadToBottom();
  };

  const appendUserMessage = (text) => {
    appendMessage(text, "user");
  };

  const appendBotMessage = (payload) => {
    appendMessage(payload.answer || "", "bot", payload.suggestions || [], payload.appliedFilters || []);
  };

  if (toggle && panel) {
    toggle.addEventListener("click", () => {
      const isOpen = toggle.getAttribute("aria-expanded") === "true";
      setOpen(!isOpen);
      if (!isOpen) {
        input.focus();
      }
    });
  }

  const submitQuery = async (message) => {
    const content = (message || input.value || "").trim();
    if (!content) {
      setStatus("Thiếu nội dung");
      return;
    }

    setOpen(true);
    appendUserMessage(content);
    submit.disabled = true;
    input.disabled = true;
    setStatus("Đang phân tích");
    input.value = "";

    try {
      const response = await fetch("/api/assistant/apartments", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ message: content })
      });

      if (!response.ok) {
        throw new Error("Assistant request failed");
      }

      const payload = await response.json();
      appendBotMessage(payload);
      setStatus("Hoàn tất");
    } catch (error) {
      appendBotMessage({
        answer: "Không thể xử lý yêu cầu lúc này. Vui lòng thử lại.",
        appliedFilters: [],
        suggestions: []
      });
      setStatus("Lỗi");
    } finally {
      submit.disabled = false;
      input.disabled = false;
      input.focus();
    }
  };

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    await submitQuery();
  });

  prompts.forEach((prompt) => {
    prompt.addEventListener("click", async () => {
      const message = prompt.dataset.assistantPrompt || "";
      input.value = message;
      await submitQuery(message);
    });
  });
}

document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("[data-table]").forEach(initRealtimeTable);
  document.querySelectorAll("[data-chat-root]").forEach(initChat);
  document.querySelectorAll("[data-apartment-assistant]").forEach(initApartmentAssistant);
  initChatUnreadBadge();
  initComparison();
});
