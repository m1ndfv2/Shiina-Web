(function () {
    "use strict";

    const root = document.querySelector("[data-supporter-keys-page]");
    if (!root) {
        return;
    }

    const dictionaries = {
        en: {
            redeemLoading: "Activating your key...",
            redeemSuccess: "Key activated successfully. Supporter is active until {date}.",
            redeemUsed: "This key has already been used.",
            redeemInvalid: "Invalid key format or key not found.",
            redeemAuth: "Authorization required. Please log in and try again.",
            redeemUnknown: "Something went wrong. Please try again in a moment.",
            pasteUnavailable: "Clipboard access is not available in this browser.",
            copiedAll: "All keys copied to clipboard.",
            loadedList: "List updated.",
            loadListError: "Unable to load keys list.",
            generated: "Generated {count} keys successfully."
        },
        ru: {
            redeemLoading: "Активируем ключ...",
            redeemSuccess: "Ключ активирован. Саппортер активен до {date}.",
            redeemUsed: "Этот ключ уже использован.",
            redeemInvalid: "Неверный формат ключа или ключ не найден.",
            redeemAuth: "Требуется авторизация. Войдите в аккаунт и повторите попытку.",
            redeemUnknown: "Что-то пошло не так. Попробуйте снова чуть позже.",
            pasteUnavailable: "Доступ к буферу обмена недоступен в этом браузере.",
            copiedAll: "Все ключи скопированы в буфер обмена.",
            loadedList: "Список обновлён.",
            loadListError: "Не удалось загрузить список ключей.",
            generated: "Успешно сгенерировано ключей: {count}."
        }
    };

    const locale = root.dataset.locale === "ru" ? "ru" : "en";
    const t = dictionaries[locale];

    const KEY_REGEX = /^[A-Z0-9]{8}-[A-Z0-9]{8}-[A-Z0-9]{8}$/;

    function normalizeKey(value) {
        const cleaned = (value || "").toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 24);
        const parts = cleaned.match(/.{1,8}/g) || [];
        return parts.join("-");
    }

    function setAlert(alertEl, variant, text) {
        if (!alertEl) {
            return;
        }

        alertEl.classList.remove("d-none", "alert-success", "alert-danger", "alert-warning", "alert-info");
        alertEl.classList.add(`alert-${variant}`);
        alertEl.textContent = text;
    }

    function setButtonLoading(buttonEl, spinnerEl, textEl, isLoading, idleText) {
        if (!buttonEl || !spinnerEl || !textEl) {
            return;
        }

        buttonEl.disabled = isLoading;
        spinnerEl.classList.toggle("d-none", !isLoading);
        textEl.textContent = isLoading ? t.redeemLoading : idleText;
    }

    async function parseJson(response) {
        try {
            return await response.json();
        } catch (_) {
            return {};
        }
    }

    function mapRedeemError(responseStatus, payload) {
        const reason = payload?.reason || payload?.error || "";
        if (responseStatus === 401 || reason === "auth_required") {
            return t.redeemAuth;
        }
        if (reason === "already_used") {
            return t.redeemUsed;
        }
        if (reason === "invalid_format" || reason === "not_found") {
            return t.redeemInvalid;
        }
        return t.redeemUnknown;
    }

    function initUserRedeemPage() {
        const form = document.getElementById("supporterRedeemForm");
        if (!form) {
            return;
        }

        const input = document.getElementById("supporterKeyInput");
        const alertEl = document.getElementById("supporterRedeemAlert");
        const submitBtn = document.getElementById("supporterRedeemSubmit");
        const spinnerEl = document.getElementById("supporterRedeemSpinner");
        const submitTextEl = document.getElementById("supporterRedeemSubmitText");
        const pasteBtn = document.getElementById("pasteSupporterKeyButton");
        const endpoint = root.dataset.redeemEndpoint;
        const idleButtonText = submitTextEl.textContent;

        input.addEventListener("input", function () {
            input.value = normalizeKey(input.value);
            input.classList.toggle("is-invalid", input.value.length > 0 && !KEY_REGEX.test(input.value));
        });

        pasteBtn?.addEventListener("click", async function () {
            if (!navigator.clipboard) {
                setAlert(alertEl, "warning", t.pasteUnavailable);
                return;
            }

            try {
                const content = await navigator.clipboard.readText();
                input.value = normalizeKey(content);
                input.dispatchEvent(new Event("input", { bubbles: true }));
                input.focus();
            } catch (_) {
                setAlert(alertEl, "warning", t.pasteUnavailable);
            }
        });

        form.addEventListener("submit", async function (event) {
            event.preventDefault();

            const code = normalizeKey(input.value);
            input.value = code;

            if (!KEY_REGEX.test(code)) {
                input.classList.add("is-invalid");
                return;
            }

            input.classList.remove("is-invalid");
            setButtonLoading(submitBtn, spinnerEl, submitTextEl, true, idleButtonText);
            setAlert(alertEl, "info", t.redeemLoading);

            try {
                const response = await fetch(endpoint, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ code: code })
                });

                const payload = await parseJson(response);

                if (response.ok) {
                    const date = payload.expiresAt || payload.expires_at || payload.expires || "-";
                    setAlert(alertEl, "success", t.redeemSuccess.replace("{date}", date));
                    form.reset();
                    return;
                }

                setAlert(alertEl, "danger", mapRedeemError(response.status, payload));
            } catch (_) {
                setAlert(alertEl, "danger", t.redeemUnknown);
            } finally {
                setButtonLoading(submitBtn, spinnerEl, submitTextEl, false, idleButtonText);
            }
        });
    }

    function statusBadge(isUsed) {
        return isUsed
            ? '<span class="badge text-bg-warning">USED</span>'
            : '<span class="badge text-bg-success">UNUSED</span>';
    }

    function safeText(value) {
        if (value === null || value === undefined || value === "") {
            return "—";
        }
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/\"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function buildQuery(params) {
        const query = new URLSearchParams();
        Object.keys(params).forEach(function (key) {
            const value = params[key];
            if (value !== null && value !== undefined && value !== "") {
                query.set(key, value);
            }
        });
        return query.toString();
    }

    function initAdminPage() {
        const generateForm = document.getElementById("supporterGenerateForm");
        if (!generateForm) {
            return;
        }

        const generateAlert = document.getElementById("supporterGenerateAlert");
        const generateButton = document.getElementById("supporterGenerateSubmit");
        const generateSpinner = document.getElementById("supporterGenerateSpinner");
        const generateText = document.getElementById("supporterGenerateSubmitText");
        const batchOutput = document.getElementById("supporterBatchOutput");
        const copyAllBtn = document.getElementById("copyAllKeysButton");
        const tableBody = document.getElementById("supporterKeysTableBody");
        const tableAlert = document.getElementById("supporterTableAlert");
        const filterForm = document.getElementById("supporterTableFilters");
        const prevPageBtn = document.getElementById("supporterPrevPage");
        const nextPageBtn = document.getElementById("supporterNextPage");
        const paginationInfo = document.getElementById("supporterPaginationInfo");

        const generateEndpoint = root.dataset.generateEndpoint;
        const listEndpoint = root.dataset.listEndpoint;
        const idleButtonText = generateText.textContent;

        const state = {
            page: 1,
            hasNextPage: false
        };

        generateForm.addEventListener("submit", async function (event) {
            event.preventDefault();

            const amountInput = document.getElementById("supporterAmount");
            const durationInput = document.getElementById("supporterDurationDays");
            const noteInput = document.getElementById("supporterNote");

            const amount = Number(amountInput.value);
            const durationDays = Number(durationInput.value);

            const amountValid = Number.isInteger(amount) && amount >= 1 && amount <= 1000;
            const durationValid = Number.isInteger(durationDays) && durationDays >= 1 && durationDays <= 3650;

            amountInput.classList.toggle("is-invalid", !amountValid);
            durationInput.classList.toggle("is-invalid", !durationValid);

            if (!amountValid || !durationValid) {
                return;
            }

            setButtonLoading(generateButton, generateSpinner, generateText, true, idleButtonText);

            try {
                const response = await fetch(generateEndpoint, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        amount: amount,
                        durationDays: durationDays,
                        note: noteInput.value.trim()
                    })
                });

                const payload = await parseJson(response);
                if (!response.ok) {
                    setAlert(generateAlert, "danger", payload.error || t.redeemUnknown);
                    return;
                }

                const keys = payload.keys || [];
                batchOutput.value = keys.join("\n");
                setAlert(generateAlert, "success", t.generated.replace("{count}", String(keys.length)));
                loadTable();
            } catch (_) {
                setAlert(generateAlert, "danger", t.redeemUnknown);
            } finally {
                setButtonLoading(generateButton, generateSpinner, generateText, false, idleButtonText);
            }
        });

        copyAllBtn?.addEventListener("click", async function () {
            if (!navigator.clipboard || !batchOutput.value.trim()) {
                return;
            }

            await navigator.clipboard.writeText(batchOutput.value);
            setAlert(generateAlert, "info", t.copiedAll);
        });

        async function loadTable() {
            const search = document.getElementById("supporterSearch").value.trim();
            const status = document.getElementById("supporterStatusFilter").value;
            const sort = document.getElementById("supporterSort").value;

            const query = buildQuery({
                page: state.page,
                search: search,
                status: status,
                sort: sort
            });

            try {
                const response = await fetch(`${listEndpoint}?${query}`);
                const payload = await parseJson(response);

                if (!response.ok) {
                    throw new Error("failed");
                }

                const items = payload.items || payload.keys || [];
                state.hasNextPage = Boolean(payload.hasNextPage || payload.has_next_page);

                tableBody.innerHTML = items
                    .map(function (item) {
                        const isUsed = Boolean(item.used || item.status === "used" || item.used_at);
                        return `<tr>
                            <td class="font-monospace">${safeText(item.code)}</td>
                            <td>${safeText(item.duration || item.durationDays)}d</td>
                            <td>${safeText(item.created_by || item.createdBy)}</td>
                            <td>${safeText(item.created_at || item.createdAt)}</td>
                            <td>${safeText(item.used_by || item.usedBy)}</td>
                            <td>${safeText(item.used_at || item.usedAt)}</td>
                            <td>${statusBadge(isUsed)}</td>
                        </tr>`;
                    })
                    .join("");

                if (!items.length) {
                    tableBody.innerHTML = '<tr><td colspan="7" class="text-center text-body-secondary py-4">No keys found.</td></tr>';
                }

                setAlert(tableAlert, "info", t.loadedList);
                paginationInfo.textContent = `Page ${state.page}`;
                prevPageBtn.disabled = state.page <= 1;
                nextPageBtn.disabled = !state.hasNextPage;
            } catch (_) {
                setAlert(tableAlert, "danger", t.loadListError);
            }
        }

        filterForm.addEventListener("submit", function (event) {
            event.preventDefault();
            state.page = 1;
            loadTable();
        });

        prevPageBtn.addEventListener("click", function () {
            if (state.page <= 1) {
                return;
            }

            state.page -= 1;
            loadTable();
        });

        nextPageBtn.addEventListener("click", function () {
            if (!state.hasNextPage) {
                return;
            }

            state.page += 1;
            loadTable();
        });

        loadTable();
    }

    if (root.dataset.supporterKeysPage === "user") {
        initUserRedeemPage();
    }

    if (root.dataset.supporterKeysPage === "admin") {
        initAdminPage();
    }
})();
