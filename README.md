# AI Worksheet Generator

Paste content copied from your LMS, choose a page count, and get back a
formatted academic worksheet as both a `.docx` and a `.pdf`.

## Prerequisites

- **Java 21** (JDK)
- **Maven 3.9+** (or use the included setup if you already have Maven)
- **LibreOffice** installed, with `soffice` (or `soffice.exe`) reachable —
  used to convert the generated DOCX to PDF. Install it from
  https://www.libreoffice.org/download/ if you don't have it.
- **[Ollama](https://ollama.com)** running locally with a model pulled, e.g.
  `ollama pull qwen2.5:3b` — this is the default provider and needs no API
  key. (A hosted OpenAI-compatible provider is also supported if you ever
  want it, but it's optional — see below.)
- A simple static file server for the frontend (e.g. the VS Code
  "Live Server" extension, or Python's built-in server).

## 1. Backend setup

```bash
cd backend
cp ../.env.example .env   # then edit .env with your real values
```

Set the same values as environment variables before running (Spring Boot
reads `AI_PROVIDER`, `OLLAMA_API_URL`, `OLLAMA_MODEL`, `AI_API_KEY`,
`LLM_TIMEOUT_SECONDS`, `LLM_MAX_TOKENS`, `LLM_MAX_RETRIES`, `SOFFICE_PATH`,
etc. from the environment — see `.env.example` for the full list and
`src/main/resources/application.yml` for how they're wired in, under the
`app.ai.*` / `app.pdf.*` / `app.cors.*` prefixes).

**Local Ollama (default — no key needed):**
```bash
ollama serve
ollama pull qwen2.5:3b
export AI_PROVIDER=ollama
export OLLAMA_API_URL=http://localhost:11434/api/chat
export OLLAMA_MODEL=qwen2.5:3b
# AI_API_KEY stays empty for Ollama
```
If you don't export anything, these are already the defaults baked into
`application.yml` — Ollama running on `localhost:11434` with `qwen2.5:3b`
works out of the box.

**Optional — a hosted OpenAI-compatible provider instead:**
```bash
export AI_PROVIDER=openai
export OLLAMA_API_URL=https://api.openai.com/v1/chat/completions   # reused as the generic api-url
export OLLAMA_MODEL=gpt-4o-mini
export AI_API_KEY=sk-...
```

Build and run:

```bash
mvn clean package
mvn spring-boot:run
```

The backend starts on `http://localhost:8080`. Confirm it's up:

```bash
curl http://localhost:8080/api/health
```

## 2. Frontend setup

The frontend is plain HTML/CSS/JS — no build step. Serve the `frontend/`
folder with any static server, for example:

```bash
cd frontend
python3 -m http.server 5500
```

Then open `http://localhost:5500` in your browser.

If your backend runs on a different host/port, edit `frontend/js/config.js`.

## 3. How to use

1. **Save your profile** (Name, UID, Class) once — it's stored in your
   browser's `localStorage` and auto-fills every future worksheet.
2. Fill in **Subject**, **Subject Code**, **Date**, and pick a
   **Number of Pages** (2–6).
3. Paste your LMS content into the textarea and click **Generate Worksheet**.
4. Review the AI-structured content in the **Preview & Edit** section —
   change anything you like, or click **Regenerate** to ask the AI again.
5. Click **Create Word & PDF**, then use **Download DOCX** / **Download PDF**.

## Generating Word and PDF

- The `.docx` is built directly with Apache POI (`DocxGeneratorService`) —
  A4 page, header/footer with dynamic page numbers, bordered code blocks,
  and page breaks placed according to your selected page count.
- The `.pdf` is produced by converting that exact `.docx` with headless
  LibreOffice (`PdfConversionService`), so the two files stay visually
  consistent.

## Common errors

| Symptom | Likely cause / fix |
|---|---|
| Navbar shows "Backend unreachable" | Backend isn't running, or is on a different port than `frontend/js/config.js` points at. |
| "AI service is not configured on the server" | `AI_API_KEY` missing, but only matters if `AI_PROVIDER` is set to something other than `ollama`. Local Ollama doesn't need one. |
| "Make sure Ollama is running…" / "Could not reach the AI service…" | `ollama serve` isn't running, or `OLLAMA_API_URL` doesn't match where it's listening (default `http://localhost:11434/api/chat`). |
| "The AI service took too long to respond (timeout=...)" | qwen2.5 is generating slowly on your hardware — raise `LLM_TIMEOUT_SECONDS` (default 600) rather than switching providers. |
| "The AI service returned an unexpected response" | The model didn't return valid JSON after 2 attempts — try a different/larger model, or click Regenerate. |
| "PDF conversion failed…" | LibreOffice isn't installed, or `SOFFICE_PATH` doesn't point at the `soffice` binary. |
| CORS error in the browser console | `CORS_ALLOWED_ORIGINS` on the backend doesn't match the origin your static server actually uses (check the port). |

## Project structure

```
AI-Worksheet-Generator/
├── backend/                  Spring Boot 3 (Java 21, Maven)
│   └── src/main/java/com/university/worksheet/
│       ├── config/            AI + PDF + CORS configuration (env-driven)
│       ├── controller/        REST endpoints
│       ├── dto/                Request/response schemas
│       ├── ai/                 Prompt building + AI HTTP call + orchestration
│       ├── document/           DOCX generation, DOCX→PDF conversion, styling
│       └── exception/          Centralized error handling
├── frontend/                  Plain HTML/CSS/JS, no build step
│   ├── index.html
│   ├── css/style.css
│   └── js/{config.js, app.js}
├── generated/                 (scratch dir; not committed)
├── .env.example
└── .gitignore
```

## Notes on scope (V1)

- Profile is stored in the browser only (`localStorage`) — no database, no
  authentication, per the "don't overengineer V1" brief.
- Very large pastes are truncated at `AI_MAX_CONTENT_CHARS` (default 60,000
  characters) rather than silently failing; raise it if you need more.
- No reference worksheet file was supplied for this build, so the DOCX/PDF
  layout follows a clean, standard academic worksheet template (see
  `DocumentStyle.java`) rather than a specific institution's exact format —
  every value that controls it lives in that one file if you want to adjust
  fonts, colors, margins, or header/footer text.
