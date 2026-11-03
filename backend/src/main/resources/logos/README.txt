Place the two exact logo image files here with these exact names:

  university-logo.png   (Chandigarh University / University Institute of Computing logo)
  naac-logo.png         (NAAC A+ logo)

DocxGeneratorService loads them from the classpath at
"logos/university-logo.png" and "logos/naac-logo.png" and embeds them
directly into the DOCX header (so they also appear in the converted PDF),
preserving aspect ratio. If a file is missing, generation still succeeds -
that side of the header falls back to plain text instead of failing.

No image files were provided when this project was generated, so this
folder currently only contains this placeholder note.
