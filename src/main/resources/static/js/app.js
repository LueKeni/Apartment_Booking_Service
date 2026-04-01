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
      // Updated color classes here
      bubble.className = `max-w-[80%] rounded-2xl px-4 py-2 text-sm shadow-sm ${
        isSelf ? "bg-indigo-600 text-white" : "bg-white text-slate-700 ring-1 ring-slate-200"
      }`;

      const content = document.createElement("p");
      content.textContent = message.content;

      const meta = document.createElement("p");
      meta.className = `mt-1 text-[10px] ${isSelf ? "text-indigo-200" : "text-slate-400"}`;
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
      // Updated color classes here
      button.className = `w-full rounded-xl px-4 py-3 text-left transition-all ${
        isActive
          ? "bg-indigo-600 text-white shadow-md"
          : "bg-white text-slate-700 hover:bg-slate-50 ring-1 ring-slate-200"
      }`;

      const name = document.createElement("p");
      name.className = "text-sm font-semibold line-clamp-1";
      name.textContent = conversation.otherUserName || "Conversation";

      const listing = document.createElement("p");
      listing.className = `mt-1 text-xs ${isActive ? "text-white/70" : "text-slate-500"}`;
      listing.textContent = conversation.apartmentTitle || "Listing";

      const meta = document.createElement("p");
      meta.className = `mt-2 text-[10px] ${isActive ? "text-indigo-300" : "text-slate-400"}`;
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

function initNotificationCenter() {
  const root = document.querySelector("[data-notification-root]");
  const badges = Array.from(document.querySelectorAll("[data-notification-unread-badge]"));
  if (!root || !badges.length) {
    return;
  }

  const toggleButton = root.querySelector("[data-notification-toggle]");
  const panel = root.querySelector("[data-notification-panel]");
  const list = root.querySelector("[data-notification-list]");
  const empty = root.querySelector("[data-notification-empty]");
  const markAllButton = root.querySelector("[data-notification-mark-all]");

  if (!toggleButton || !panel || !list || !empty) {
    return;
  }

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

  const setBadges = (count) => {
    const display = Number.isFinite(count) && count > 99 ? "99+" : String(count || 0);
    badges.forEach((badge) => {
      if (count > 0) {
        badge.textContent = display;
        badge.classList.remove("hidden");
      } else {
        badge.classList.add("hidden");
      }
    });
  };

  const markRead = async (notificationId) => {
    await fetch(`/api/notifications/${notificationId}/read`, { method: "POST" });
  };

  const refreshUnreadCount = async () => {
    try {
      const response = await fetch("/api/notifications/unread-count");
      if (!response.ok) {
        throw new Error("Failed to load unread count");
      }
      const unread = Number(await response.json()) || 0;
      setBadges(unread);
    } catch (error) {
      // Keep stale badge data on transient failures.
    }
  };

  const renderNotifications = (notifications) => {
    list.innerHTML = "";
    const hasItems = Array.isArray(notifications) && notifications.length > 0;
    empty.classList.toggle("hidden", hasItems);
    if (!hasItems) {
      return;
    }

    notifications.forEach((notification) => {
      const row = document.createElement("a");
      row.href = notification.openUrl || notification.actionUrl || "/user/notifications";
      row.className = `block rounded-xl border border-slate-200 p-3 transition-colors hover:bg-slate-50 ${
        notification.isRead ? "bg-white" : "bg-sky-50/60"
      }`;

      row.innerHTML = `
        <p class="text-sm font-bold text-slate-900">${notification.title || "Notification"}</p>
        <p class="mt-1 text-xs text-slate-600">${notification.message || ""}</p>
        <div class="mt-2 flex items-center justify-between gap-2">
          <span class="inline-flex items-center rounded-full border border-slate-200 bg-white px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-slate-500">${notification.type || "SYSTEM"}</span>
          <span class="text-[10px] text-slate-500">${formatTimestamp(notification.createdAt)}</span>
        </div>
      `;

      if (!notification.isRead && notification.id != null) {
        row.addEventListener("click", () => {
          markRead(notification.id).catch(() => {
            // Notification open action can continue even if mark-read fails.
          });
        });
      }

      list.appendChild(row);
    });
  };

  const refreshNotifications = async () => {
    try {
      const response = await fetch("/api/notifications");
      if (!response.ok) {
        throw new Error("Failed to load notifications");
      }
      const notifications = await response.json();
      renderNotifications(notifications);
    } catch (error) {
      list.innerHTML = "";
      empty.classList.remove("hidden");
    }
  };

  const openPanel = async () => {
    panel.classList.remove("hidden");
    toggleButton.setAttribute("aria-expanded", "true");
    await refreshNotifications();
    await refreshUnreadCount();
  };

  const closePanel = () => {
    panel.classList.add("hidden");
    toggleButton.setAttribute("aria-expanded", "false");
  };

  toggleButton.addEventListener("click", async (event) => {
    event.preventDefault();
    if (panel.classList.contains("hidden")) {
      await openPanel();
    } else {
      closePanel();
    }
  });

  document.addEventListener("click", (event) => {
    if (!panel.classList.contains("hidden") && !root.contains(event.target)) {
      closePanel();
    }
  });

  if (markAllButton) {
    markAllButton.addEventListener("click", async () => {
      try {
        await fetch("/api/notifications/read-all", { method: "POST" });
        await refreshNotifications();
        await refreshUnreadCount();
      } catch (error) {
        // Ignore transient network errors and keep current UI.
      }
    });
  }

  refreshUnreadCount();
  setInterval(refreshUnreadCount, 10000);
  document.addEventListener("visibilitychange", () => {
    if (!document.hidden) {
      refreshUnreadCount();
      if (!panel.classList.contains("hidden")) {
        refreshNotifications().catch(() => {
          // Keep UI stable if refresh fails.
        });
      }
    }
  });
}

function initApartmentCompare() {
  const addButtons = Array.from(document.querySelectorAll("[data-compare-add]"));
  if (!addButtons.length) {
    return;
  }

  const bar = document.getElementById("comparison-bar");
  const countLabel = document.getElementById("compare-count");
  const itemsContainer = document.getElementById("compare-items");
  const clearButton = document.getElementById("clear-compare");
  const compareNowButton = document.getElementById("compare-now-btn");
  const modal = document.getElementById("comparison-modal");
  const modalContent = document.getElementById("comparison-content");
  const closeModalButton = document.getElementById("close-comparison-modal");

  if (!bar || !countLabel || !itemsContainer || !clearButton || !compareNowButton || !modal || !modalContent || !closeModalButton) {
    return;
  }

  let compareList = [];

  const updateUI = () => {
    countLabel.textContent = `${compareList.length}/3`;
    bar.classList.toggle("translate-y-full", compareList.length === 0);
    compareNowButton.disabled = compareList.length < 2;

    itemsContainer.innerHTML = "";
    compareList.forEach((item) => {
      const card = document.createElement("div");
      card.className = "relative group h-11 w-11";
      card.innerHTML = `
        <img src="${item.image}" alt="${item.title}" class="h-11 w-11 rounded-lg border border-slate-200 object-cover" />
        <button type="button" class="absolute -right-1 -top-1 hidden h-4 w-4 items-center justify-center rounded-full bg-rose-500 text-[10px] text-white group-hover:flex" data-compare-remove="${item.id}">
          <i class="fas fa-times"></i>
        </button>
      `;
      card.querySelector("[data-compare-remove]")?.addEventListener("click", () => {
        compareList = compareList.filter((current) => current.id !== item.id);
        updateUI();
      });
      itemsContainer.appendChild(card);
    });
  };

  const addToCompare = (item) => {
    if (!item.id) {
      return;
    }
    if (compareList.some((existing) => existing.id === item.id)) {
      return;
    }
    if (compareList.length >= 3) {
      alert("Ban chi co the so sanh toi da 3 can ho.");
      return;
    }
    compareList.push(item);
    updateUI();
  };

  const renderComparisonTable = () => {
    if (compareList.length < 2) {
      return;
    }

    modalContent.innerHTML = `
      <div class="overflow-x-auto">
        <table class="min-w-[680px] w-full border-collapse text-left text-sm">
          <thead>
            <tr>
              <th class="border border-slate-200 bg-slate-50 p-3 font-bold text-slate-700">Tieu chi</th>
              ${compareList
                .map(
                  (item) => `
                    <th class="border border-slate-200 bg-slate-50 p-3 text-center">
                      <img src="${item.image}" alt="${item.title}" class="mx-auto mb-2 h-20 w-32 rounded-lg object-cover" />
                      <p class="line-clamp-2 font-semibold text-slate-900">${item.title}</p>
                    </th>
                  `
                )
                .join("")}
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="border border-slate-200 p-3 font-semibold bg-slate-50">Gia</td>
              ${compareList.map((item) => `<td class="border border-slate-200 p-3 text-center font-bold text-midnight-700">${item.price || "-"}</td>`).join("")}
            </tr>
            <tr>
              <td class="border border-slate-200 p-3 font-semibold bg-slate-50">Khu vuc</td>
              ${compareList.map((item) => `<td class="border border-slate-200 p-3 text-center">${item.district || "-"}</td>`).join("")}
            </tr>
            <tr>
              <td class="border border-slate-200 p-3 font-semibold bg-slate-50">Loai phong</td>
              ${compareList.map((item) => `<td class="border border-slate-200 p-3 text-center">${item.type || "-"}</td>`).join("")}
            </tr>
            <tr>
              <td class="border border-slate-200 p-3 font-semibold bg-slate-50">Dien tich</td>
              ${compareList.map((item) => `<td class="border border-slate-200 p-3 text-center">${item.area || "-"} m2</td>`).join("")}
            </tr>
            <tr>
              <td class="border border-slate-200 p-3 font-semibold bg-slate-50">Chi tiet</td>
              ${compareList
                .map(
                  (item) => `
                    <td class="border border-slate-200 p-3 text-center">
                      <a href="/apartments/${item.id}" class="inline-flex rounded-lg bg-midnight-700 px-3 py-2 text-xs font-bold text-white hover:bg-midnight-600">Xem can ho</a>
                    </td>
                  `
                )
                .join("")}
            </tr>
          </tbody>
        </table>
      </div>
    `;
  };

  addButtons.forEach((button) => {
    button.addEventListener("click", () => {
      const data = button.dataset;
      addToCompare({
        id: data.compareAdd,
        title: data.compareTitle || "Apartment",
        price: data.comparePrice || "-",
        image: data.compareImage || "/images/apartment-placeholder.svg",
        district: data.compareDistrict || "-",
        type: data.compareType || "-",
        area: data.compareArea || "-"
      });
    });
  });

  clearButton.addEventListener("click", () => {
    compareList = [];
    updateUI();
  });

  compareNowButton.addEventListener("click", () => {
    renderComparisonTable();
    modal.classList.remove("hidden");
  });

  closeModalButton.addEventListener("click", () => {
    modal.classList.add("hidden");
  });

  modal.addEventListener("click", (event) => {
    if (event.target === modal) {
      modal.classList.add("hidden");
    }
  });

  updateUI();
}

function initApartmentAssistant(root) {
  const toggleButton = root.querySelector("[data-assistant-toggle]");
  const panel = root.querySelector("[data-assistant-panel]");
  const status = root.querySelector("[data-assistant-status]");
  const thread = root.querySelector("[data-assistant-thread]");
  const promptButtons = Array.from(root.querySelectorAll("[data-assistant-prompt]"));
  const form = root.querySelector("[data-assistant-form]");
  const input = root.querySelector("[data-assistant-input]");
  const submitButton = root.querySelector("[data-assistant-submit]");

  if (!toggleButton || !panel || !status || !thread || !form || !input || !submitButton) {
    return;
  }

  const setStatus = (text) => {
    status.textContent = text;
  };

  const setBusy = (busy) => {
    input.disabled = busy;
    submitButton.disabled = busy;
    submitButton.classList.toggle("opacity-60", busy);
    submitButton.classList.toggle("cursor-not-allowed", busy);
  };

  const appendMessage = ({ role, text, filters, suggestions }) => {
    const row = document.createElement("div");
    row.className = `assistant-row ${role === "user" ? "assistant-row-user" : "assistant-row-bot"}`;

    const bubble = document.createElement("div");
    bubble.className = `assistant-bubble ${role === "user" ? "assistant-bubble-user" : "assistant-bubble-bot"}`;

    const content = document.createElement("p");
    content.textContent = text || "";
    bubble.appendChild(content);

    if (Array.isArray(filters) && filters.length > 0) {
      const filterWrap = document.createElement("div");
      filterWrap.className = "assistant-filter-wrap";
      filters.forEach((filter) => {
        const item = document.createElement("span");
        item.className = "assistant-filter-chip";
        item.textContent = filter;
        filterWrap.appendChild(item);
      });
      bubble.appendChild(filterWrap);
    }

    if (Array.isArray(suggestions) && suggestions.length > 0) {
      const suggestionWrap = document.createElement("div");
      suggestionWrap.className = "assistant-suggestions";

      suggestions.forEach((suggestion) => {
        const card = document.createElement("a");
        card.className = "assistant-suggestion-card";
        card.href = suggestion.detailUrl || "#";

        const image = document.createElement("img");
        image.src = suggestion.imageUrl || "/images/apartment-placeholder.svg";
        image.alt = suggestion.title || "Apartment";
        image.className = "assistant-suggestion-image";

        const info = document.createElement("div");
        info.className = "assistant-suggestion-info";

        const title = document.createElement("p");
        title.className = "assistant-suggestion-title";
        title.textContent = suggestion.title || "Apartment";

        const meta = document.createElement("p");
        meta.className = "assistant-suggestion-meta";
        const district = suggestion.district || "-";
        const roomType = suggestion.roomType || "-";
        meta.textContent = `${district} • ${roomType}`;

        const price = document.createElement("p");
        price.className = "assistant-suggestion-price";
        price.textContent = suggestion.priceLabel || "Contact";

        info.appendChild(title);
        info.appendChild(meta);
        info.appendChild(price);

        card.appendChild(image);
        card.appendChild(info);
        suggestionWrap.appendChild(card);
      });

      bubble.appendChild(suggestionWrap);
    }

    row.appendChild(bubble);
    thread.appendChild(row);
    thread.scrollTop = thread.scrollHeight;
  };

  const sendMessage = async (message) => {
    const trimmed = (message || "").trim();
    if (!trimmed) {
      return;
    }

    appendMessage({ role: "user", text: trimmed });
    setStatus("Thinking...");
    setBusy(true);

    try {
      const response = await fetch("/api/assistant/apartments", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ message: trimmed })
      });

      if (!response.ok) {
        throw new Error("Assistant request failed");
      }

      const payload = await response.json();
      appendMessage({
        role: "bot",
        text: payload.answer || "I could not process that request.",
        filters: payload.appliedFilters || [],
        suggestions: payload.suggestions || []
      });
      setStatus("Ready");
    } catch (error) {
      appendMessage({
        role: "bot",
        text: "Assistant is temporarily unavailable. Please try again in a moment."
      });
      setStatus("Offline");
    } finally {
      setBusy(false);
      input.focus();
    }
  };

  toggleButton.addEventListener("click", () => {
    const willOpen = panel.classList.contains("hidden");
    panel.classList.toggle("hidden", !willOpen);
    toggleButton.setAttribute("aria-expanded", willOpen ? "true" : "false");
    if (willOpen) {
      input.focus();
    }
  });

  promptButtons.forEach((button) => {
    button.addEventListener("click", () => {
      sendMessage(button.dataset.assistantPrompt || "");
    });
  });

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    const message = input.value;
    input.value = "";
    sendMessage(message);
  });
}

function initImageFallbacks() {
  const apartmentFallback = "/images/apartment-placeholder.svg";
  const avatarFallback = "/images/avatar-placeholder.svg";

  document.querySelectorAll("img").forEach((img) => {
    img.addEventListener("error", () => {
      if (img.dataset.fallbackApplied === "1") {
        return;
      }
      img.dataset.fallbackApplied = "1";

      const isAvatar = img.classList.contains("rounded-full") || !!img.closest(".rounded-full");
      img.src = isAvatar ? avatarFallback : apartmentFallback;
      img.classList.add("image-fallback");
    });
  });
}

function initHeroSlider(root) {
  const slides = Array.from(root.querySelectorAll("[data-hero-slide]"));
  const indicators = Array.from(root.querySelectorAll("[data-hero-indicator]"));
  const prevButton = root.querySelector("[data-hero-prev]");
  const nextButton = root.querySelector("[data-hero-next]");

  if (slides.length <= 1) {
    return;
  }

  const intervalMs = Number(root.dataset.interval) || 5000;
  let current = 0;
  let timerId = null;

  const setActive = (index) => {
    current = (index + slides.length) % slides.length;
    slides.forEach((slide, slideIndex) => {
      slide.classList.toggle("is-active", slideIndex === current);
    });
    indicators.forEach((indicator, indicatorIndex) => {
      indicator.classList.toggle("is-active", indicatorIndex === current);
      indicator.setAttribute("aria-current", indicatorIndex === current ? "true" : "false");
    });
  };

  const next = () => setActive(current + 1);
  const prev = () => setActive(current - 1);

  const start = () => {
    if (timerId) {
      clearInterval(timerId);
    }
    timerId = setInterval(next, intervalMs);
  };

  const stop = () => {
    if (timerId) {
      clearInterval(timerId);
      timerId = null;
    }
  };

  indicators.forEach((indicator) => {
    indicator.addEventListener("click", () => {
      const index = Number(indicator.dataset.index);
      if (Number.isFinite(index)) {
        setActive(index);
        start();
      }
    });
  });

  prevButton?.addEventListener("click", () => {
    prev();
    start();
  });

  nextButton?.addEventListener("click", () => {
    next();
    start();
  });

  root.addEventListener("mouseenter", stop);
  root.addEventListener("mouseleave", start);

  setActive(0);
  start();
}

document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("[data-hero-slider]").forEach(initHeroSlider);
  document.querySelectorAll("[data-table]").forEach(initRealtimeTable);
  document.querySelectorAll("[data-chat-root]").forEach(initChat);
  if (typeof initApartmentAssistant === "function") {
    document.querySelectorAll("[data-apartment-assistant]").forEach(initApartmentAssistant);
  }
  initChatUnreadBadge();
  initNotificationCenter();
  initApartmentCompare();
  initImageFallbacks();
});