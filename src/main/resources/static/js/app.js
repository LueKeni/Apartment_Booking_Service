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

document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("[data-table]").forEach(initRealtimeTable);
});
