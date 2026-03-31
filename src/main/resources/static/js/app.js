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
        : "Listing chat";
      return;
    }
    if (state.recipientId && state.apartmentId) {
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
      listing.className = `mt-1 text-xs line-clamp-1 ${isActive ? "text-indigo-200" : "text-slate-500"}`;
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
    state.apartmentId = conversation.apartmentId;
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
      setComposerEnabled(Boolean(state.recipientId && state.apartmentId));
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
      } else if (state.recipientId && state.apartmentId) {
        const matched = cachedConversations.find(
          (item) => item.otherUserId === state.recipientId && item.apartmentId === state.apartmentId
        );
        if (matched) {
          selectConversation(matched);
          return;
        }
      }

      updateHeader();
      setComposerEnabled(Boolean(state.recipientId && state.apartmentId));
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
    if (!state.recipientId || !state.apartmentId) {
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

  const toggle = root.querySelector("[data-notification-toggle]");
  const panel = root.querySelector("[data-notification-panel]");
  const list = root.querySelector("[data-notification-list]");
  const empty = root.querySelector("[data-notification-empty]");
  const markAllButton = root.querySelector("[data-notification-mark-all]");

  if (!toggle || !panel || !list || !empty) {
    return;
  }

  let lastUnreadCount = 0;

  const setUnreadBadges = (count) => {
    lastUnreadCount = count;
    const display = Number.isFinite(count) && count > 99 ? "99+" : String(count || 0);
    badges.forEach((badge) => {
      if (count && count > 0) {
        badge.textContent = display;
        badge.classList.remove("hidden");
      } else {
        badge.classList.add("hidden");
      }
    });
  };

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

  const markRead = async (notificationId) => {
    await fetch(`/api/notifications/${notificationId}/read`, { method: "POST" });
  };

  const renderNotifications = (notifications) => {
    list.innerHTML = "";
    const hasItems = Array.isArray(notifications) && notifications.length > 0;
    empty.classList.toggle("hidden", hasItems);
    if (!hasItems) {
      return;
    }

    notifications.slice(0, 8).forEach((notification) => {
      const item = document.createElement("article");
      // Updated color classes here
      item.className = `rounded-xl p-3 ${notification.isRead ? "bg-white ring-1 ring-slate-200" : "bg-indigo-50 ring-1 ring-indigo-200"}`;

      const title = document.createElement("p");
      title.className = "text-sm font-semibold text-slate-900 line-clamp-1";
      title.textContent = notification.title || "Notification";

      const message = document.createElement("p");
      message.className = "mt-1 text-xs text-slate-600 line-clamp-2";
      message.textContent = notification.message || "";

      const metaRow = document.createElement("div");
      metaRow.className = "mt-2 flex items-center justify-between gap-2";

      const createdAt = document.createElement("span");
      createdAt.className = "text-[10px] font-medium text-slate-500";
      createdAt.textContent = formatTimestamp(notification.createdAt);

      metaRow.appendChild(createdAt);

      if (!notification.isRead) {
        const readButton = document.createElement("button");
        readButton.type = "button";
        readButton.className = "rounded-md bg-white px-2.5 py-1 text-[11px] font-semibold text-slate-700 shadow-sm ring-1 ring-inset ring-slate-300 hover:bg-slate-50";
        readButton.textContent = "Mark read";
        readButton.addEventListener("click", async () => {
          try {
            await markRead(notification.id);
            await refreshNotifications();
          } catch (error) {
            // Ignore transient network errors and keep current UI.
          }
        });
        metaRow.appendChild(readButton);
      }

      item.appendChild(title);
      item.appendChild(message);
      item.appendChild(metaRow);
      list.appendChild(item);
    });
  };

  const refreshUnreadCount = async () => {
    try {
      const response = await fetch("/api/notifications/unread-count");
      if (!response.ok) {
        throw new Error("Failed to load unread notifications");
      }
      const count = Number(await response.json());
      setUnreadBadges(Number.isFinite(count) ? count : 0);
    } catch (error) {
      setUnreadBadges(0);
    }
  };

  const refreshNotifications = async () => {
    try {
      const response = await fetch("/api/notifications");
      if (!response.ok) {
        throw new Error("Failed to load notifications");
      }
      const notifications = await response.json();
      renderNotifications(notifications);
      const unreadCount = notifications.filter((notification) => !notification.isRead).length;
      setUnreadBadges(unreadCount);
    } catch (error) {
      renderNotifications([]);
      if (lastUnreadCount === 0) {
        setUnreadBadges(0);
      }
    }
  };

  const openPanel = async () => {
    panel.classList.remove("hidden");
    toggle.setAttribute("aria-expanded", "true");
    await refreshNotifications();
  };

  const closePanel = () => {
    panel.classList.add("hidden");
    toggle.setAttribute("aria-expanded", "false");
  };

  toggle.addEventListener("click", async (event) => {
    event.stopPropagation();
    if (panel.classList.contains("hidden")) {
      await openPanel();
    } else {
      closePanel();
    }
  });

  document.addEventListener("click", (event) => {
    if (!root.contains(event.target)) {
      closePanel();
    }
  });

  if (markAllButton) {
    markAllButton.addEventListener("click", async () => {
      try {
        await fetch("/api/notifications/read-all", { method: "POST" });
        await refreshNotifications();
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
        refreshNotifications();
      }
    }
  });
}

document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("[data-table]").forEach(initRealtimeTable);
  document.querySelectorAll("[data-chat-root]").forEach(initChat);
  initChatUnreadBadge();
  initNotificationCenter();
});