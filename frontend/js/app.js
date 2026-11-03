(function () {
  "use strict";

  const API_BASE = window.APP_CONFIG.API_BASE_URL;
  const PROFILE_KEY = "worksheet.profile";
  const EXPERIMENT_KEY = "worksheet.experimentNumber";

  // ---------------------------------------------------------------- state
  let selectedPageCount = 4;
  let currentContent = null;   // structured content returned by /analyze (and then edited)
  let lastCodeFileName = "";   // preserves the original filename when only one code file is edited
  let docxBlob = null;
  let pdfBlob = null;

  // ------------------------------------------------------------- elements
  const el = (id) => document.getElementById(id);

  const profileForm = el("profileForm");
  const profileDisplay = el("profileDisplay");
  const editProfileBtn = el("editProfileBtn");

  const generateBtn = el("generateBtn");
  const generateBtnLabel = el("generateBtnLabel");
  const clearBtn = el("clearBtn");
  const progressPanel = el("progressPanel");
  const progressText = el("progressText");
  const errorPanel = el("errorPanel");

  const previewCard = el("previewCard");
  const resultCard = el("resultCard");

  const regenerateBtn = el("regenerateBtn");
  const createFilesBtn = el("createFilesBtn");

  const downloadDocxBtn = el("downloadDocxBtn");
  const downloadPdfBtn = el("downloadPdfBtn");

  // ------------------------------------------------------------------ init

  document.addEventListener("DOMContentLoaded", () => {
    initDateDefault();
    initExperimentNumber();
    initProfile();
    initPageCountSegmented();
    checkBackendHealth();
  });

  function initDateDefault() {
    const dateInput = el("worksheetDate");
    if (!dateInput.value) {
      dateInput.value = new Date().toISOString().slice(0, 10);
    }
  }

  function initExperimentNumber() {
    const hasStored = localStorage.getItem(EXPERIMENT_KEY) !== null;
    const stored = parseInt(localStorage.getItem(EXPERIMENT_KEY) || "3", 10);
    // Fresh install (nothing stored yet) defaults to Worksheet-4; after that,
    // each generated worksheet increments from the last one used.
    const next = hasStored ? stored + 1 : 4;
    el("experimentNumber").value = String(next).padStart(2, "0");
  }

  function initPageCountSegmented() {
    const container = el("pageCountSegmented");
    container.querySelectorAll(".segmented__option").forEach((btn) => {
      btn.addEventListener("click", () => {
        container.querySelectorAll(".segmented__option").forEach((b) =>
          b.classList.remove("segmented__option--active")
        );
        btn.classList.add("segmented__option--active");
        selectedPageCount = parseInt(btn.dataset.pages, 10);
      });
    });
  }

  async function checkBackendHealth() {
    const dot = el("apiStatusDot");
    const text = el("apiStatusText");
    try {
      const res = await fetch(`${API_BASE}/api/health`, { method: "GET" });
      if (res.ok) {
        dot.classList.add("status-dot--ok");
        text.textContent = "Backend connected";
      } else {
        throw new Error("unhealthy");
      }
    } catch (e) {
      dot.classList.add("status-dot--error");
      text.textContent = "Backend unreachable";
    }
  }

  // --------------------------------------------------------------- profile

  function initProfile() {
    const saved = getSavedProfile();
    if (saved) {
      showProfileDisplay(saved);
    } else {
      showProfileForm();
    }
  }

  function getSavedProfile() {
    try {
      const raw = localStorage.getItem(PROFILE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  }

  function showProfileDisplay(profile) {
    el("displayName").textContent = profile.name;
    el("displayUid").textContent = profile.uid;
    el("displayClass").textContent = profile.className;
    profileDisplay.classList.remove("hidden");
    profileForm.classList.add("hidden");
  }

  function showProfileForm(prefill) {
    profileDisplay.classList.add("hidden");
    profileForm.classList.remove("hidden");
    if (prefill) {
      el("profileName").value = prefill.name || "";
      el("profileUid").value = prefill.uid || "";
      el("profileClass").value = prefill.className || "";
    }
  }

  profileForm.addEventListener("submit", (e) => {
    e.preventDefault();
    const profile = {
      name: el("profileName").value.trim(),
      uid: el("profileUid").value.trim(),
      className: el("profileClass").value.trim(),
    };
    if (!profile.name || !profile.uid || !profile.className) return;
    localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
    showProfileDisplay(profile);
  });

  editProfileBtn.addEventListener("click", () => showProfileForm(getSavedProfile()));

  // --------------------------------------------------------------- errors

  function showError(message) {
    errorPanel.textContent = message;
    errorPanel.classList.remove("hidden");
  }

  function clearError() {
    errorPanel.classList.add("hidden");
    errorPanel.textContent = "";
  }

  function showProgress(message) {
    progressText.textContent = message;
    progressPanel.classList.remove("hidden");
  }

  function hideProgress() {
    progressPanel.classList.add("hidden");
  }

  // --------------------------------------------------------------- clear

  clearBtn.addEventListener("click", () => {
    el("rawContent").value = "";
    clearError();
  });

  // ------------------------------------------------------------- analyze

  generateBtn.addEventListener("click", () => runAnalyze());
  regenerateBtn.addEventListener("click", () => runAnalyze());

  async function runAnalyze() {
    clearError();

    const rawContent = el("rawContent").value.trim();
    if (rawContent.length < 20) {
      showError("Please paste the LMS content before generating a worksheet.");
      return;
    }
    const profile = getSavedProfile();
    if (!profile) {
      showError("Please save your profile (Name, UID, Class) first.");
      return;
    }

    setGenerating(true);
    try {
      showProgress("Analyzing LMS content…");
      await delay(300); // lets the first stage register visually
      showProgress("Generating worksheet…");

      const response = await fetch(`${API_BASE}/api/worksheet/analyze`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          rawContent,
          subjectHint: el("subject").value.trim(),
          pageCount: selectedPageCount,
        }),
      });

      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || "Unable to generate worksheet.");
      }

      currentContent = data;
      lastCodeFileName = (data.codeImplementation && data.codeImplementation[0] && data.codeImplementation[0].fileName) || "";
      populatePreview(data);
      previewCard.classList.remove("hidden");
      resultCard.classList.add("hidden");
      previewCard.scrollIntoView({ behavior: "smooth", block: "start" });
    } catch (err) {
      showError(err.message || "Unable to generate worksheet. Please make sure the backend and AI provider are running.");
    } finally {
      hideProgress();
      setGenerating(false);
    }
  }

  function setGenerating(isGenerating) {
    generateBtn.disabled = isGenerating;
    generateBtnLabel.textContent = isGenerating ? "Working…" : "Generate Worksheet";
  }

  function delay(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  // ------------------------------------------------------------- preview

  function populatePreview(content) {
    el("editTitle").value = content.experimentTitle || "";
    el("editAim").value = content.aim || "";
    el("editObjectives").value = (content.objectives || []).join("\n");
    el("editInput").value = (content.input || []).join("\n");
    el("editApparatus").value = (content.apparatusUsed || []).join("\n");
    el("editCode").value = (content.codeImplementation || []).map((f) => f.code).join("\n\n");
    el("editOutput").value = content.output || "";
    el("editLearningOutcomes").value = (content.learningOutcomes || []).join("\n");

    el("previewTitle").textContent = content.experimentTitle || "Untitled Experiment";
    el("previewMetaSubject").textContent = el("subject").value.trim() || "Subject";
    const profile = getSavedProfile();
    el("previewMetaName").textContent = profile ? profile.name : "";
    el("previewMetaDate").textContent = el("worksheetDate").value;
  }

  function linesFrom(textareaId) {
    return el(textareaId)
      .value.split("\n")
      .map((s) => s.trim())
      .filter((s) => s.length > 0);
  }

  function buildContentFromEdits() {
    return {
      experimentTitle: el("editTitle").value.trim(),
      aim: el("editAim").value.trim(),
      objectives: linesFrom("editObjectives"),
      input: linesFrom("editInput"),
      apparatusUsed: linesFrom("editApparatus"),
      codeImplementation: el("editCode").value.trim()
        ? [{ fileName: lastCodeFileName || "", code: el("editCode").value }]
        : [],
      output: el("editOutput").value.trim(),
      learningOutcomes: linesFrom("editLearningOutcomes"),
    };
  }

  // --------------------------------------------------------- create files

  createFilesBtn.addEventListener("click", () => createFiles());

  async function createFiles() {
    clearError();
    const profile = getSavedProfile();
    if (!profile) {
      showError("Please save your profile first.");
      return;
    }

    const requestBody = {
      name: profile.name,
      uid: profile.uid,
      className: profile.className,
      subject: el("subject").value.trim(),
      subjectCode: el("subjectCode").value.trim(),
      date: el("worksheetDate").value,
      experimentNumber: el("experimentNumber").value.trim(),
      branch: el("branch").value.trim(),
      sectionGroup: el("sectionGroup").value.trim(),
      semester: el("semester").value.trim(),
      pageCount: selectedPageCount,
      content: buildContentFromEdits(),
    };

    createFilesBtn.disabled = true;
    try {
      showProgress("Creating DOCX and PDF…");

      const [docxRes, pdfRes] = await Promise.all([
        fetch(`${API_BASE}/api/document/docx`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(requestBody),
        }),
        fetch(`${API_BASE}/api/document/pdf`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(requestBody),
        }),
      ]);

      if (!docxRes.ok) throw new Error(await extractError(docxRes, "Failed to generate the Word document."));
      if (!pdfRes.ok) throw new Error(await extractError(pdfRes, "Failed to generate the PDF."));

      docxBlob = await docxRes.blob();
      pdfBlob = await pdfRes.blob();

      // increment stored experiment number for next time
      const num = parseInt(el("experimentNumber").value, 10);
      if (!isNaN(num)) localStorage.setItem(EXPERIMENT_KEY, String(num));

      el("resultPages").textContent = String(selectedPageCount);
      el("resultSubject").textContent = requestBody.subject || "—";
      el("resultSubjectCode").textContent = requestBody.subjectCode || "—";
      el("resultDate").textContent = requestBody.date || "—";

      resultCard.classList.remove("hidden");
      resultCard.scrollIntoView({ behavior: "smooth", block: "start" });
    } catch (err) {
      showError(err.message || "Unable to generate the worksheet files. Please make sure the backend and LibreOffice are running.");
    } finally {
      hideProgress();
      createFilesBtn.disabled = false;
    }
  }

  async function extractError(response, fallback) {
    try {
      const data = await response.json();
      return data.message || fallback;
    } catch (e) {
      return fallback;
    }
  }

  downloadDocxBtn.addEventListener("click", () => {
    if (docxBlob) downloadBlob(docxBlob, buildFilename("docx"));
  });

  downloadPdfBtn.addEventListener("click", () => {
    if (pdfBlob) downloadBlob(pdfBlob, buildFilename("pdf"));
  });

  function buildFilename(extension) {
    const subject = (el("subject").value.trim() || "Worksheet").replace(/\s+/g, "_");
    const number = el("experimentNumber").value.trim();
    return `${subject}${number ? "_Experiment_" + number : ""}.${extension}`;
  }

  function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }
})();
