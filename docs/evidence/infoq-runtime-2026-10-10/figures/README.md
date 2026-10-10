# Article figures

These original diagrams accompany [the English manuscript](../java-runtime-android-native-image.md). Each has an editable SVG and a 2x PNG embedded in the Markdown. Labels explain arrow meaning; colors aid grouping and are not needed to interpret the diagrams. The diagrams are conceptual views, not exhaustive class, module, or deployment graphs.

| Figure | Purpose | Source basis |
| --- | --- | --- |
| 01-platform-boundary | Spring coupling before the refactor; shared contracts and separate providers afterward | June 2026 history, current core/runtime and provider sources |
| 02-expression-execution | Corresponding Q operations through local server/Android contexts, returning values for E | Example bootstrap and query code, expression generator, execution adapters |
| 03-mapping-output-boundaries | Explicit entity construction and hydration; independent JSON, audit, and reference boundaries | PortableSQLRepository, EntityDescriptor, generated entity access, teaql-jackson, audit and reference runtime |

The manuscript links the relevant source evidence and explains version differences. Figure 3 shows output-side relationships only; it is not a full codec round-trip or document protocol. The optimized row-mapping path depends on projection and provider capabilities.

To regenerate SVG and PNG files, run `python3 render_figures.py` with Pillow installed. The script uses the Arial fonts supplied by macOS; adjust FONT and BOLD when rendering on another system. SVG files can also be edited directly in a vector editor. PNGs are 2400 pixels wide and suitable as separate editorial assets. Confirm any final format conversion with the InfoQ editor.
