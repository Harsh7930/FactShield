# FactShield

Developed FactShield, a Java-based fake news detection system featuring a Swing GUI, keyword-based backend analysis, and integrated testing. The team collaboratively designed, implemented, and validated the system, with comprehensive presentation covering overall functionality and project workflow.

## UI Features

- **Duplicate-free title** — The app name appears only in the window title bar (`FactShield`); the main panel shows a single subtitle (“Content Credibility Analyzer”) with no second large heading.
- **Descriptive verdict messages** — Results use HTML in the analysis panel: for **Fake**, a warning (⚠️) with *“This article is likely AI-generated or may contain misleading information.”*; for **Real**, a checkmark (✅) with *“This article appears to be credible news content.”*; on failure, *“Analysis unavailable – please check Python AI script.”*
- **Confidence line and color-coded bar** — The model confidence is shown as *“Confidence: N%”*, with a progress bar colored **green** (≥70%), **yellow** (30–69%), or **red** (<30%). A small gray caption under the bar explains the scale.
- **Keyboard shortcuts** — **Ctrl+Enter** runs analysis, **Ctrl+V** pastes from the clipboard, **Esc** clears input and the result area.
- **Dark mode** — Toggle light/dark theme (FlatLaf) without restarting.
- **Character counter** — Live `current / 500` count for the input text area.

## Try it

From the project root (after a successful build):

```bash
./build.sh && ./run.sh
```

### Example expectations

Paste or type sample headlines and click **Analyze** (or **Ctrl+Enter**).

| Kind of input | What you should see in the result panel |
|---------------|----------------------------------------|
| Sensational / AI-like headline | ⚠️ message about AI-generated or misleading content; **Confidence:** *N*%; bar often yellow or red depending on *N*. |
| Neutral, factual-style headline | ✅ message about credible news content; **Confidence:** *N*%; bar may be green if *N* ≥ 70%. |
| Working app but detector failure | Plain error text: *Analysis unavailable – please check Python AI script.* Gray bar. |

Example sensational line to try:

> “Shocking discovery: scientists stunned by unbelievable revelation!”

You should see the ⚠️ fake-style message and a **Confidence:** percentage line consistent with the model output.
