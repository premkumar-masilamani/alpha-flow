#!/usr/bin/env python3
import os
import re
import json
import sys

# Paths
BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
JAVA_SRC_DIR = os.path.join(BASE_DIR, "backend", "src", "main", "java")
DIAGRAMS_DIR = os.path.join(BASE_DIR, "docs")

def strip_comments(text):
    # Strip block comments /* ... */
    text = re.sub(r'/\*.*?\*/', '', text, flags=re.DOTALL)
    # Strip line comments // ...
    text = re.sub(r'//.*', '', text)
    return text

def parse_java_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    cleaned = strip_comments(content)

    # Extract package
    pkg_match = re.search(r'package\s+([a-zA-Z0-9_\.]+);', cleaned)
    package = pkg_match.group(1) if pkg_match else ""

    # Class Name is filename without extension
    class_name = os.path.basename(filepath).replace(".java", "")
    fqn = f"{package}.{class_name}" if package else class_name

    # Determine component (api, engine, persistence, root)
    if "com.alphaflow.api" in package:
        component = "api"
    elif "com.alphaflow.engine" in package:
        component = "engine"
    elif "com.alphaflow.persistence" in package:
        component = "persistence"
    else:
        component = "root"

    # Extract imports
    imports = []
    for line in cleaned.splitlines():
        line = line.strip()
        if line.startswith("import ") and line.endswith(";"):
            imp = line[7:-1].strip()
            # Clean modifier like 'static'
            if imp.startswith("static "):
                imp = imp[7:].strip()
            imports.append(imp)

    # Class type
    class_type = "class"
    type_match = re.search(r'\b(interface|class|enum|record)\s+' + re.escape(class_name) + r'\b', cleaned)
    if type_match:
        class_type = type_match.group(1)

    return {
        "name": class_name,
        "fqn": fqn,
        "package": package,
        "component": component,
        "type": class_type,
        "filepath": os.path.relpath(filepath, BASE_DIR),
        "imports": imports,
        "content": cleaned
    }

def main():
    if not os.path.exists(JAVA_SRC_DIR):
        print(f"Error: Java source directory not found at {JAVA_SRC_DIR}", file=sys.stderr)
        sys.exit(1)

    # 1. Walk Java files
    java_files = []
    for root, _, files in os.walk(JAVA_SRC_DIR):
        for file in files:
            if file.endswith(".java"):
                java_files.append(os.path.join(root, file))

    classes = []
    for filepath in java_files:
        try:
            classes.append(parse_java_file(filepath))
        except Exception as e:
            print(f"Warning: Failed to parse {filepath}: {e}", file=sys.stderr)

    # Map for FQN lookup and list of all class names
    class_map = {c['fqn']: c for c in classes}
    class_names = {c['name'] for c in classes}

    # 2. Resolve dependencies
    dependencies = []

    def resolve_class_name(name, src_class):
        candidates = [c for c in classes if c['name'] == name]
        if not candidates:
            return None

        # Exact package match
        for cand in candidates:
            if cand['package'] == src_class['package']:
                return cand['fqn']

        # Explicit import match
        for cand in candidates:
            if cand['fqn'] in src_class['imports']:
                return cand['fqn']

        # Wildcard import match
        for cand in candidates:
            wildcard = cand['package'] + ".*"
            if wildcard in src_class['imports']:
                return cand['fqn']

        # Full FQN usage check in content
        for cand in candidates:
            if cand['fqn'] in src_class['content']:
                return cand['fqn']

        # If there's only one candidate and it matches FQN in imports/content
        if len(candidates) == 1:
            cand = candidates[0]
            # If the class name is used, and it's a unique name in the codebase,
            # check if it is imported or in the same package (handled above), or if it is fully qualified.
            # Otherwise we don't assume dependency to avoid false positives.
            pass

        return None

    # Build edges
    edges = set() # Set of (source_fqn, target_fqn)

    for c in classes:
        # Find all words in content
        words = set(re.findall(r'\b[A-Za-z0-9_]+\b', c['content']))

        # Direct imports (specific class or star imports)
        for imp in c['imports']:
            if imp.startswith("com.alphaflow"):
                if imp.endswith(".*"):
                    # Add all classes in that package
                    pkg = imp[:-2]
                    for target in classes:
                        if target['package'] == pkg and target['fqn'] != c['fqn']:
                            edges.add((c['fqn'], target['fqn']))
                else:
                    if imp in class_map and imp != c['fqn']:
                        edges.add((c['fqn'], imp))

        # Same package and textual reference resolution
        for word in words:
            if word in class_names and word != c['name']:
                resolved = resolve_class_name(word, c)
                if resolved and resolved != c['fqn']:
                    edges.add((c['fqn'], resolved))

    # 3. Validate Rules & Detect Violations
    # api -> engine, persistence (allowed)
    # engine -> persistence (allowed), engine -> api (VIOLATION)
    # persistence -> api (VIOLATION), persistence -> engine (VIOLATION)
    violations = []
    violated_nodes = set()
    violated_edges = []

    edges_list = []
    for src, tgt in sorted(edges):
        src_c = class_map[src]
        tgt_c = class_map[tgt]

        is_violation = False
        reason = ""

        if src_c['component'] == 'engine' and tgt_c['component'] == 'api':
            is_violation = True
            reason = "Engine component cannot depend on API layer DTOs or Controllers"
        elif src_c['component'] == 'persistence' and tgt_c['component'] == 'api':
            is_violation = True
            reason = "Persistence component cannot depend on API layer DTOs or Controllers"
        elif src_c['component'] == 'persistence' and tgt_c['component'] == 'engine':
            is_violation = True
            reason = "Persistence database entities and repositories cannot depend on Business Logic (Engine)"

        if is_violation:
            violations.append({
                "source": src,
                "target": tgt,
                "reason": reason,
                "source_file": src_c['filepath'],
                "target_file": tgt_c['filepath']
            })
            violated_nodes.add(src)
            violated_nodes.add(tgt)

        edges_list.append({
            "source": src,
            "target": tgt,
            "violation": is_violation,
            "reason": reason
        })

    # Ensure output directories exist
    os.makedirs(DIAGRAMS_DIR, exist_ok=True)

    # 4. Generate JSON Graph
    graph_data = {
        "nodes": [
            {
                "id": c['fqn'],
                "name": c['name'],
                "package": c['package'],
                "component": c['component'],
                "type": c['type'],
                "filepath": c['filepath'],
                "violation": c['fqn'] in violated_nodes
            }
            for c in classes
        ],
        "edges": edges_list,
        "violations": violations
    }

    with open(os.path.join(DIAGRAMS_DIR, "class_map.json"), "w", encoding="utf-8") as f:
        json.dump(graph_data, f, indent=2)

    # 5. Generate Markdown Report (Mermaid)
    generate_markdown_report(classes, edges_list, violations)

    # 6. Generate Interactive HTML (Cytoscape.js)
    generate_html_visualizer(classes, edges_list, violations)

    # Print summary to stdout
    print(f"Successfully processed {len(classes)} classes and {len(edges_list)} dependencies.")
    print(f"Generated:")
    print(f"  - JSON Data: architecture/diagrams/class_map.json")
    print(f"  - Markdown:  architecture/diagrams/class_map.md")
    print(f"  - HTML App:  architecture/diagrams/index.html")

    if violations:
        print(f"\n[WARNING] Found {len(violations)} package separation violations!", file=sys.stderr)
        for v in violations:
            print(f"  - {v['source']} -> {v['target']}", file=sys.stderr)
            print(f"    Reason: {v['reason']}", file=sys.stderr)
            print(f"    Source File: {v['source_file']}", file=sys.stderr)

    # Handle check mode
    if "--check" in sys.argv:
        if violations:
            print(f"\nValidation failed: {len(violations)} architectural violations found.", file=sys.stderr)
            sys.exit(1)
        else:
            print("\nValidation passed: No package separation violations found.")
            sys.exit(0)

def generate_markdown_report(classes, edges, violations):
    md_path = os.path.join(DIAGRAMS_DIR, "class_map.md")

    with open(md_path, "w", encoding="utf-8") as f:
        f.write("# AlphaFlow Class Map & Package Dependency Report\n\n")
        f.write("This report was generated dynamically by parsing the backend codebase. It provides a visual class dependency map and flags any violations of package separation rules.\n\n")

        # Violations section
        if violations:
            f.write("## ⚠️ Architectural Violations\n\n")
            f.write("> [!WARNING]\n")
            f.write(f"> Found **{len(violations)}** architectural violations where package boundaries have been breached.\n\n")
            f.write("| Source Class | Target Dependency | Reason | Source File |\n")
            f.write("| :--- | :--- | :--- | :--- |\n")
            for v in violations:
                f.write(f"| `{v['source'].split('.')[-1]}` ({v['source']}) | `{v['target'].split('.')[-1]}` ({v['target']}) | {v['reason']} | [`{os.path.basename(v['source_file'])}`](file://{os.path.join(BASE_DIR, v['source_file'])}#L1) |\n")
            f.write("\n---\n\n")
        else:
            f.write("## ✅ Package Separation Status\n\n")
            f.write("> [!NOTE]\n")
            f.write("> All classes adhere to the clean separation boundaries. No violations detected.\n\n")

        # Mermaid Diagram
        f.write("## Class Dependency Diagram (Mermaid)\n\n")
        f.write("```mermaid\nflowchart TD\n")

        # Subgraphs for components
        components = {"api": "API Layer", "engine": "Engine (Logic)", "persistence": "Persistence (Database)"}
        for comp, label in components.items():
            f.write(f"    subgraph {comp} [\"{label}\"]\n")
            for c in classes:
                if c['component'] == comp:
                    f.write(f"        {c['fqn'].replace('.', '_')}[\"{c['name']} ({c['type']})\"]\n")
            f.write("    end\n\n")

        # Add root classes
        for c in classes:
            if c['component'] == "root":
                f.write(f"    {c['fqn'].replace('.', '_')}[\"{c['name']} (Root)\"]\n")

        f.write("\n    %% Edges\n")
        # Write edges
        violation_indices = []
        for idx, edge in enumerate(edges):
            src_id = edge['source'].replace('.', '_')
            tgt_id = edge['target'].replace('.', '_')

            arrow = "-->"
            if edge['violation']:
                arrow = "==x"
                violation_indices.append(idx)

            f.write(f"    {src_id} {arrow} {tgt_id}\n")

        # Style violation links
        f.write("\n    %% Styles for Violations\n")
        for idx in violation_indices:
            f.write(f"    linkStyle {idx} stroke:#ef4444,stroke-width:3px;\n")

        # Style violation nodes
        violated_fqns = {v['source'] for v in violations} | {v['target'] for v in violations}
        for fqn in violated_fqns:
            f.write(f"    classDef violated stroke:#ef4444,stroke-width:2px,fill:#fee2e2,color:#991b1b;\n")
            f.write(f"    class {fqn.replace('.', '_')} violated;\n")

        f.write("```\n\n")

        # Summary details
        f.write("## Component Summary\n\n")
        f.write(f"- **Total Classes**: {len(classes)}\n")
        f.write(f"- **Total Dependencies**: {len(edges)}\n")
        f.write(f"- **API Layer Classes**: {len([c for c in classes if c['component'] == 'api'])}\n")
        f.write(f"- **Engine Layer Classes**: {len([c for c in classes if c['component'] == 'engine'])}\n")
        f.write(f"- **Persistence Layer Classes**: {len([c for c in classes if c['component'] == 'persistence'])}\n")

def generate_html_visualizer(classes, edges, violations):
    html_path = os.path.join(DIAGRAMS_DIR, "index.html")

    # Generate cytoscape elements JSON
    cy_elements = []

    # 1. Compound Component Nodes
    components = [
        {"id": "api", "label": "API LAYER (controllers, dtos, mappers, services)"},
        {"id": "engine", "label": "ENGINE LAYER (calculators, downloaders, indicators, schedulers)"},
        {"id": "persistence", "label": "PERSISTENCE LAYER (entities, repositories, enums)"}
    ]
    for comp in components:
        cy_elements.append({
            "data": {
                "id": comp['id'],
                "label": comp['label'],
                "is_parent": True
            }
        })

    # 2. Package nodes (grouped under components)
    packages = sorted(list({c['package'] for c in classes if c['package']}))
    for pkg in packages:
        # Find which component it belongs to
        parent_comp = "root"
        if "com.alphaflow.api" in pkg:
            parent_comp = "api"
        elif "com.alphaflow.engine" in pkg:
            parent_comp = "engine"
        elif "com.alphaflow.persistence" in pkg:
            parent_comp = "persistence"

        cy_elements.append({
            "data": {
                "id": pkg,
                "label": pkg.replace("com.alphaflow.", ""),
                "parent": parent_comp,
                "is_parent": True
            }
        })

    # 3. Class Nodes
    violated_fqns = {v['source'] for v in violations} | {v['target'] for v in violations}
    for c in classes:
        cy_elements.append({
            "data": {
                "id": c['fqn'],
                "label": c['name'],
                "parent": c['package'] if c['package'] else None,
                "type": c['type'],
                "component": c['component'],
                "filepath": c['filepath'],
                "violation": c['fqn'] in violated_fqns
            }
        })

    # 4. Edges
    for edge in edges:
        cy_elements.append({
            "data": {
                "id": f"{edge['source']}_to_{edge['target']}",
                "source": edge['source'],
                "target": edge['target'],
                "violation": edge['violation'],
                "reason": edge['reason']
            }
        })

    # Sleek dark-mode HTML template with Cytoscape.js and Dagre Layout
    html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AlphaFlow Class Dependency Map</title>
    <!-- Cytoscape.js and Dagre Hierarchical Layout -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/cytoscape/3.29.2/cytoscape.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/dagre@0.8.5/dist/dagre.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/cytoscape-dagre@2.5.0/cytoscape-dagre.min.js"></script>
    <style>
        :root {{
            --bg-color: #0b0f19;
            --panel-bg: rgba(17, 24, 39, 0.85);
            --border-color: rgba(255, 255, 255, 0.1);
            --text-primary: #f3f4f6;
            --text-secondary: #9ca3af;
            --accent-api: #3b82f6;
            --accent-engine: #10b981;
            --accent-persistence: #fb8c00;
            --accent-violation: #ef4444;
            --accent-root: #6b7280;
        }}

        * {{
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }}

        body {{
            background-color: var(--bg-color);
            color: var(--text-primary);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            height: 100vh;
            overflow: hidden;
            display: flex;
        }}

        #cy {{
            flex: 1;
            height: 100%;
            z-index: 1;
        }}

        /* Sidebar Glassmorphism */
        .sidebar {{
            width: 420px;
            height: 100%;
            background-color: var(--panel-bg);
            backdrop-filter: blur(12px);
            -webkit-backdrop-filter: blur(12px);
            border-left: 1px solid var(--border-color);
            display: flex;
            flex-direction: column;
            z-index: 10;
            box-shadow: -4px 0 24px rgba(0, 0, 0, 0.5);
        }}

        .sidebar-header {{
            padding: 24px;
            border-bottom: 1px solid var(--border-color);
        }}

        .sidebar-header h1 {{
            font-size: 20px;
            font-weight: 700;
            margin-bottom: 6px;
            background: linear-gradient(90deg, #60a5fa, #34d399);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }}

        .sidebar-header p {{
            font-size: 13px;
            color: var(--text-secondary);
        }}

        .controls {{
            padding: 20px 24px;
            border-bottom: 1px solid var(--border-color);
            display: flex;
            flex-direction: column;
            gap: 16px;
        }}

        .filter-group {{
            display: flex;
            flex-direction: column;
            gap: 6px;
        }}

        .search-container {{
            position: relative;
        }}

        .search-input {{
            width: 100%;
            padding: 10px 14px;
            background-color: rgba(255, 255, 255, 0.05);
            border: 1px solid var(--border-color);
            border-radius: 8px;
            color: var(--text-primary);
            font-size: 14px;
            outline: none;
            transition: border-color 0.2s;
        }}

        .search-input:focus {{
            border-color: #60a5fa;
        }}

        .filter-buttons {{
            display: flex;
            gap: 8px;
        }}

        .btn {{
            flex: 1;
            padding: 8px 12px;
            background-color: rgba(255, 255, 255, 0.05);
            border: 1px solid var(--border-color);
            border-radius: 6px;
            color: var(--text-primary);
            font-size: 12px;
            cursor: pointer;
            transition: background-color 0.2s, border-color 0.2s;
        }}

        .btn:hover {{
            background-color: rgba(255, 255, 255, 0.1);
        }}

        .btn.active {{
            background-color: rgba(59, 130, 246, 0.2);
            border-color: var(--accent-api);
        }}

        .details-panel {{
            flex: 1;
            overflow-y: auto;
            padding: 24px;
            display: flex;
            flex-direction: column;
            gap: 20px;
        }}

        .detail-section {{
            display: flex;
            flex-direction: column;
            gap: 8px;
        }}

        .detail-label {{
            font-size: 11px;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: var(--text-secondary);
            font-weight: 700;
        }}

        .detail-value {{
            font-size: 14px;
            word-break: break-all;
            background-color: rgba(255, 255, 255, 0.02);
            padding: 8px 12px;
            border-radius: 6px;
            border: 1px solid rgba(255, 255, 255, 0.03);
        }}

        .violation-banner {{
            background-color: rgba(239, 68, 68, 0.15);
            border: 1px solid var(--accent-violation);
            color: #fca5a5;
            padding: 12px;
            border-radius: 8px;
            font-size: 13px;
            margin-bottom: 12px;
            display: flex;
            flex-direction: column;
            gap: 6px;
        }}

        .violation-title {{
            font-weight: 700;
            color: #ef4444;
            display: flex;
            align-items: center;
            gap: 6px;
        }}

        .badge {{
            display: inline-block;
            padding: 2px 6px;
            font-size: 11px;
            font-weight: 600;
            border-radius: 4px;
            text-transform: uppercase;
        }}

        .badge-class {{ background-color: rgba(255, 255, 255, 0.1); color: var(--text-primary); }}
        .badge-interface {{ background-color: rgba(16, 185, 129, 0.15); color: #34d399; }}
        .badge-enum {{ background-color: rgba(245, 158, 11, 0.15); color: #fbbf24; }}
        .badge-record {{ background-color: rgba(139, 92, 246, 0.15); color: #a78bfa; }}

        .legend {{
            position: absolute;
            bottom: 24px;
            left: 24px;
            background-color: var(--panel-bg);
            backdrop-filter: blur(12px);
            -webkit-backdrop-filter: blur(12px);
            border: 1px solid var(--border-color);
            padding: 16px;
            border-radius: 12px;
            z-index: 10;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
            display: flex;
            flex-direction: column;
            gap: 8px;
            font-size: 12px;
        }}

        .legend-title {{
            font-weight: bold;
            margin-bottom: 4px;
            color: var(--text-secondary);
        }}

        .legend-item {{
            display: flex;
            align-items: center;
            gap: 8px;
        }}

        .legend-color {{
            width: 12px;
            height: 12px;
            border-radius: 3px;
        }}

        .legend-line {{
            width: 20px;
            height: 2px;
        }}

        .edge-list-item {{
            cursor: pointer;
            padding: 4px 0;
            color: #60a5fa;
            text-decoration: underline;
        }}
        .edge-list-item:hover {{
            color: #93c5fd;
        }}
    </style>
</head>
<body>

    <div id="cy"></div>

    <!-- Legend Overlay -->
    <div class="legend">
        <div class="legend-title">Legend</div>
        <div class="legend-item">
            <div class="legend-color" style="background-color: var(--accent-api)"></div>
            <span>API Layer (Blue)</span>
        </div>
        <div class="legend-item">
            <div class="legend-color" style="background-color: var(--accent-engine)"></div>
            <span>Engine Layer (Green)</span>
        </div>
        <div class="legend-item">
            <div class="legend-color" style="background-color: var(--accent-persistence)"></div>
            <span>Persistence Layer (Orange)</span>
        </div>
        <div class="legend-item">
            <div class="legend-color" style="background-color: var(--accent-root)"></div>
            <span>Root (Gray)</span>
        </div>
        <div class="legend-item">
            <div class="legend-color" style="background-color: var(--accent-violation)"></div>
            <span>Violation (Red Node)</span>
        </div>
        <div class="legend-item" style="margin-top: 4px;">
            <div class="legend-line" style="background-color: #4b5563;"></div>
            <span>Allowed Dep Arrow</span>
        </div>
        <div class="legend-item">
            <div class="legend-line" style="background-color: var(--accent-violation); height: 2px; border-top: 2px dashed var(--accent-violation);"></div>
            <span>Violation Arrow (Red Dashed)</span>
        </div>
    </div>

    <!-- Sidebar Control and Details Panel -->
    <div class="sidebar">
        <div class="sidebar-header">
            <h1>AlphaFlow Architecture</h1>
            <p>Dependency Graph & Boundaries</p>
        </div>

        <div class="controls">
            <div class="search-container">
                <input type="text" id="search" class="search-input" placeholder="Search class (e.g. ASTAStrategy)">
            </div>

            <div class="filter-group">
                <div class="detail-label">Layout Mode</div>
                <select id="layout-select" class="search-input" style="font-size: 13px;">
                    <option value="dagre" selected>Layered Hierarchical (Dagre)</option>
                    <option value="cose">Force-Directed Clustered (Cose)</option>
                    <option value="grid">Structured Grid</option>
                    <option value="circle">Concentric Circle</option>
                </select>
            </div>

            <div class="filter-group">
                <div class="detail-label">Filter Components</div>
                <div style="display: flex; flex-direction: column; gap: 8px; font-size: 13px; margin-top: 2px;">
                    <label style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
                        <input type="checkbox" id="chk-api" checked> Show API Layer
                    </label>
                    <label style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
                        <input type="checkbox" id="chk-engine" checked> Show Engine Layer
                    </label>
                    <label style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
                        <input type="checkbox" id="chk-persistence" checked> Show Persistence Layer
                    </label>
                </div>
            </div>

            <div class="filter-buttons">
                <button class="btn active" id="btn-fit">Fit Visualizer</button>
                <button class="btn" id="btn-violations">Violations Only</button>
            </div>
        </div>

        <div class="details-panel" id="details">
            <div style="text-align: center; color: var(--text-secondary); margin-top: 40px;">
                <p>Click on any class node or dependency line to view details, imports, and violation analysis.</p>
            </div>
        </div>
    </div>

    <!-- Cytoscape Elements & Setup -->
    <script>
        const elements = {json.dumps(cy_elements, indent=2)};

        document.addEventListener('DOMContentLoaded', function() {{
            const cy = cytoscape({{
                container: document.getElementById('cy'),
                elements: elements,
                style: [
                    // Base Node styles
                    {{
                        selector: 'node',
                        style: {{
                            'content': 'data(label)',
                            'text-valign': 'center',
                            'text-halign': 'center',
                            'background-color': '#6b7280', // Slate Gray
                            'color': '#ffffff',
                            'font-size': '11px',
                            'font-weight': 'bold',
                            'shape': 'round-rectangle',
                            'width': 'label',
                            'height': 'label',
                            'padding': '8px 16px',
                            'border-width': '1px',
                            'border-color': '#1f2937',
                            'overlay-opacity': 0,
                            'transition-property': 'background-color, line-color, target-arrow-color, width, height, border-color, border-width, opacity',
                            'transition-duration': '0.15s'
                        }}
                    }},
                    // Compound / Group Nodes
                    {{
                        selector: 'node[?is_parent]',
                        style: {{
                            'content': 'data(label)',
                            'text-valign': 'top',
                            'text-halign': 'left',
                            'background-color': 'rgba(255, 255, 255, 0.01)',
                            'border-width': '1px',
                            'border-color': '#374151',
                            'border-style': 'dashed',
                            'shape': 'roundrectangle',
                            'color': '#9ca3af',
                            'font-size': '10px',
                            'font-weight': 'bold',
                            'padding': '24px 12px 12px 12px'
                        }}
                    }},
                    // Component Specific Bounding boxes
                    {{
                        selector: '#api',
                        style: {{
                            'border-color': 'rgba(59, 130, 246, 0.3)',
                            'border-style': 'solid',
                            'border-width': '2px',
                            'color': '#60a5fa',
                            'font-size': '13px',
                            'padding': '36px 16px 16px 16px'
                        }}
                    }},
                    {{
                        selector: '#engine',
                        style: {{
                            'border-color': 'rgba(16, 185, 129, 0.3)',
                            'border-style': 'solid',
                            'border-width': '2px',
                            'color': '#34d399',
                            'font-size': '13px',
                            'padding': '36px 16px 16px 16px'
                        }}
                    }},
                    {{
                        selector: '#persistence',
                        style: {{
                            'border-color': 'rgba(251, 140, 0, 0.3)',
                            'border-style': 'solid',
                            'border-width': '2px',
                            'color': '#fbbf24',
                            'font-size': '13px',
                            'padding': '36px 16px 16px 16px'
                        }}
                    }},
                    // Node Components styling (using explicit Hex values instead of css variables)
                    {{
                        selector: 'node[component="api"]',
                        style: {{ 'background-color': '#3b82f6' }}
                    }},
                    {{
                        selector: 'node[component="engine"]',
                        style: {{ 'background-color': '#10b981' }}
                    }},
                    {{
                        selector: 'node[component="persistence"]',
                        style: {{ 'background-color': '#fb8c00' }}
                    }},
                    // Violation Nodes
                    {{
                        selector: 'node[?violation][!is_parent]',
                        style: {{
                            'border-width': '3px',
                            'border-color': '#ef4444',
                            'background-color': '#dc2626'
                        }}
                    }},
                    // Edges styles
                    {{
                        selector: 'edge',
                        style: {{
                            'width': 1.5,
                            'line-color': '#4b5563',
                            'target-arrow-color': '#4b5563',
                            'target-arrow-shape': 'triangle',
                            'curve-style': 'bezier',
                            'arrow-scale': 1.0,
                            'opacity': 0.35,
                            'overlay-opacity': 0,
                            'transition-property': 'line-color, target-arrow-color, width, opacity',
                            'transition-duration': '0.15s'
                        }}
                    }},
                    // Violation Edges
                    {{
                        selector: 'edge[?violation]',
                        style: {{
                            'line-color': '#ef4444',
                            'target-arrow-color': '#ef4444',
                            'width': 3,
                            'opacity': 1.0,
                            'line-style': 'dashed'
                        }}
                    }},
                    // Dimmed nodes and edges for search/focus
                    {{
                        selector: '.dimmed',
                        style: {{
                            'opacity': 0.08
                        }}
                    }},
                    {{
                        selector: '.highlighted',
                        style: {{
                            'opacity': 1.0,
                            'width': 3.5,
                            'line-color': '#60a5fa',
                            'target-arrow-color': '#60a5fa'
                        }}
                    }}
                ],
                layout: {{
                    name: 'dagre',
                    rankDir: 'TB',
                    nodeSep: 60,
                    rankSep: 140,
                    animate: false
                }}
            }});

            // Keep track of violation nodes
            const violationNodes = cy.nodes('[?violation][!is_parent]');
            const violationEdges = cy.edges('[?violation]');
            let onlyViolationsMode = false;

            // Details panel reference
            const detailsPanel = document.getElementById('details');

            function showNodeDetails(node) {{
                const data = node.data();
                if (data.is_parent) return;

                // Fetch incoming and outgoing connections
                const incoming = node.incomers('edge');
                const outgoing = node.outgoers('edge');

                let violationHtml = '';
                if (data.violation) {{
                    // Find specific violations involving this node
                    const nodeViolations = elements.filter(el => el.data.violation && (el.data.source === data.id || el.data.target === data.id));

                    violationHtml = `
                        <div class="violation-banner">
                            <div class="violation-title">
                                ⚠️ Package Separation Breach
                            </div>
                            <p>This class is involved in ${{nodeViolations.length}} dependency rule violation(s):</p>
                            <ul style="padding-left: 16px; margin-top: 6px;">
                                ${{nodeViolations.map(v => `<li><strong>${{v.data.source.split('.').pop()}}</strong> references forbidden <strong>${{v.data.target.split('.').pop()}}</strong></li>`).join('')}}
                            </ul>
                        </div>
                    `;
                }}

                let incomingHtml = incoming.map(edge => {{
                    const srcName = edge.data('source').split('.').pop();
                    return `<li class="edge-list-item" onclick="focusElement('${{edge.data('id')}}')">${{srcName}}</li>`;
                }}).join('') || 'None';

                let outgoingHtml = outgoing.map(edge => {{
                    const tgtName = edge.data('target').split('.').pop();
                    const isViol = edge.data('violation') ? ' style="color:#ef4444;font-weight:bold;"' : '';
                    return `<li class="edge-list-item" onclick="focusElement('${{edge.data('id')}}')"${{isViol}}>${{tgtName}} ${{edge.data('violation') ? '⚠️' : ''}}</li>`;
                }}).join('') || 'None';

                detailsPanel.innerHTML = `
                    ${{violationHtml}}

                    <div class="detail-section">
                        <div class="detail-label">Class Name</div>
                        <div class="detail-value" style="font-weight: 700; font-size: 16px;">
                            ${{data.label}}
                            <span class="badge badge-${{data.type}}">${{data.type}}</span>
                        </div>
                    </div>

                    <div class="detail-section">
                        <div class="detail-label">Fully Qualified Name</div>
                        <div class="detail-value" style="font-family: monospace; font-size: 12px; color: #a5f3fc;">
                            ${{data.id}}
                        </div>
                    </div>

                    <div class="detail-section">
                        <div class="detail-label">File Path</div>
                        <div class="detail-value" style="font-size: 12px;">
                            ${{data.filepath}}
                        </div>
                    </div>

                    <div class="detail-section">
                        <div class="detail-label">Component Group</div>
                        <div class="detail-value" style="text-transform: uppercase; font-weight: 600; color: ${{data.component === 'api' ? '#3b82f6' : data.component === 'engine' ? '#10b981' : '#fb8c00'}};">
                            ${{data.component}}
                        </div>
                    </div>

                    <div class="detail-section">
                        <div class="detail-label">Incoming Dependents (Classes importing this)</div>
                        <div class="detail-value">
                            <ul style="padding-left: 16px;">${{incomingHtml}}</ul>
                        </div>
                    </div>

                    <div class="detail-section">
                        <div class="detail-label">Outgoing Dependencies (Classes imported by this)</div>
                        <div class="detail-value">
                            <ul style="padding-left: 16px;">${{outgoingHtml}}</ul>
                        </div>
                    </div>
                `;
            }}

            function showEdgeDetails(edge) {{
                const data = edge.data();
                const srcName = data.source.split('.').pop();
                const tgtName = data.target.split('.').pop();

                let violationHtml = '';
                if (data.violation) {{
                    violationHtml = `
                        <div class="violation-banner">
                            <div class="violation-title">
                                ⚠️ Boundary Violation
                            </div>
                            <p>${{data.reason}}</p>
                        </div>
                    `;
                }}

                detailsPanel.innerHTML = `
                    ${{violationHtml}}

                    <div class="detail-section">
                        <div class="detail-label">Dependency Line</div>
                        <div class="detail-value" style="font-weight: 700; font-size: 15px;">
                            ${{srcName}} &rarr; ${{tgtName}}
                        </div>
                    </div>

                    <div class="detail-section">
                        <div class="detail-label">Source Class</div>
                        <div class="detail-value" class="edge-list-item" onclick="focusElement('${{data.source}}')">
                            ${{data.source}}
                        </div>
                    </div>

                    <div class="detail-section">
                        <div class="detail-label">Target Class</div>
                        <div class="detail-value" class="edge-list-item" onclick="focusElement('${{data.target}}')">
                            ${{data.target}}
                        </div>
                    </div>

                    <div class="detail-section">
                        <div class="detail-label">Status</div>
                        <div class="detail-value" style="color: ${{data.violation ? '#ef4444' : '#10b981'}}; font-weight: bold;">
                            ${{data.violation ? 'FORBIDDEN (Rule Violation)' : 'ALLOWED (Valid Layering)'}}
                        </div>
                    </div>
                `;
            }}

            window.focusElement = function(id) {{
                const el = cy.getElementById(id);
                if (el.length > 0) {{
                    cy.elements().removeClass('dimmed').removeClass('highlighted');

                    if (el.isNode()) {{
                        // Highlight node and its neighbors
                        const neighbors = el.neighborhood();
                        cy.elements().difference(el).difference(neighbors).addClass('dimmed');
                        el.addClass('highlighted');
                        showNodeDetails(el);
                    }} else {{
                        // Highlight edge and its source/target
                        const nodes = el.connectedNodes();
                        cy.elements().difference(el).difference(nodes).addClass('dimmed');
                        el.addClass('highlighted');
                        showEdgeDetails(el);
                    }}
                }}
            }};

            // Node click listener
            cy.on('tap', 'node', function(evt) {{
                const node = evt.target;
                if (!node.data('is_parent')) {{
                    focusElement(node.id());
                }}
            }});

            // Edge click listener
            cy.on('tap', 'edge', function(evt) {{
                const edge = evt.target;
                focusElement(edge.id());
            }});

            // Background tap resets styles
            cy.on('tap', function(evt) {{
                if (evt.target === cy) {{
                    cy.elements().removeClass('dimmed').removeClass('highlighted');
                    detailsPanel.innerHTML = `
                        <div style="text-align: center; color: var(--text-secondary); margin-top: 40px;">
                            <p>Click on any class node or dependency line to view details, imports, and violation analysis.</p>
                        </div>
                    `;
                }}
            }});

            // Search filtering
            const searchInput = document.getElementById('search');
            searchInput.addEventListener('input', function() {{
                const q = this.value.toLowerCase().trim();
                if (!q) {{
                    cy.elements().removeClass('dimmed').removeClass('highlighted');
                    return;
                }}

                cy.elements().addClass('dimmed');

                const matches = cy.nodes().filter(node => {{
                    return !node.data('is_parent') && node.data('label').toLowerCase().includes(q);
                }});

                matches.removeClass('dimmed');
                matches.neighborhood().removeClass('dimmed');
            }});

            // Control Buttons
            document.getElementById('btn-fit').addEventListener('click', function() {{
                cy.fit();
                cy.center();
            }});

            // Filter Checkbox handling
            function applyFilters() {{
                const showApi = document.getElementById('chk-api').checked;
                const showEngine = document.getElementById('chk-engine').checked;
                const showPersistence = document.getElementById('chk-persistence').checked;

                cy.batch(() => {{
                    cy.nodes().forEach(node => {{
                        if (node.data('is_parent')) return;

                        const comp = node.data('component');
                        let isVisible = true;

                        if (comp === 'api' && !showApi) isVisible = false;
                        if (comp === 'engine' && !showEngine) isVisible = false;
                        if (comp === 'persistence' && !showPersistence) isVisible = false;
                        if (onlyViolationsMode && !node.data('violation')) isVisible = false;

                        if (isVisible) {{
                            node.style('display', 'element');
                        }} else {{
                            node.style('display', 'none');
                        }}
                    }});

                    // Filter empty package/component boundaries
                    cy.nodes('[?is_parent]').forEach(parent => {{
                        const visibleChildren = parent.descendants().filter(node => !node.data('is_parent') && node.style('display') !== 'none');
                        if (visibleChildren.length === 0) {{
                            parent.style('display', 'none');
                        }} else {{
                            parent.style('display', 'element');
                        }}
                    }});
                }});
            }}

            document.getElementById('chk-api').addEventListener('change', applyFilters);
            document.getElementById('chk-engine').addEventListener('change', applyFilters);
            document.getElementById('chk-persistence').addEventListener('change', applyFilters);

            document.getElementById('btn-violations').addEventListener('click', function() {{
                onlyViolationsMode = !onlyViolationsMode;
                if (onlyViolationsMode) {{
                    this.textContent = "Show All Graph";
                    this.classList.add('active');

                    if (violationNodes.length === 0) {{
                        cy.elements().addClass('dimmed');
                        detailsPanel.innerHTML = `
                            <div style="text-align: center; color: #10b981; margin-top: 40px;">
                                <p>🎉 Excellent! No package separation violations found.</p>
                            </div>
                        `;
                        return;
                    }}

                    applyFilters();
                }} else {{
                    this.textContent = "Violations Only";
                    this.classList.remove('active');
                    applyFilters();
                    cy.elements().removeClass('dimmed');
                }}
            }});

            // Layout switching
            const layoutSelect = document.getElementById('layout-select');

            function runLayout(layoutName) {{
                let layoutConfig = {{ name: layoutName }};
                if (layoutName === 'dagre') {{
                    layoutConfig = {{
                        name: 'dagre',
                        rankDir: 'TB',
                        nodeSep: 60,
                        rankSep: 140,
                        animate: true,
                        animationDuration: 400
                    }};
                }} else if (layoutName === 'cose') {{
                    layoutConfig = {{
                        name: 'cose',
                        idealEdgeLength: 100,
                        nodeOverlap: 20,
                        refresh: 20,
                        fit: true,
                        padding: 30,
                        randomize: true,
                        componentSpacing: 100,
                        nodeRepulsion: 400000,
                        edgeElasticity: 100,
                        nestingFactor: 5,
                        gravity: 80,
                        numIter: 1000
                    }};
                }} else if (layoutName === 'grid') {{
                    layoutConfig = {{
                        name: 'grid',
                        fit: true,
                        padding: 30,
                        avoidOverlap: true,
                        columns: 8
                    }};
                }} else if (layoutName === 'circle') {{
                    layoutConfig = {{
                        name: 'circle',
                        fit: true,
                        padding: 30
                    }};
                }}
                cy.layout(layoutConfig).run();
            }}

            layoutSelect.addEventListener('change', function() {{
                runLayout(this.value);
            }});
        }});
    </script>
</body>
</html>
"""
    with open(html_path, "w", encoding="utf-8") as f:
        f.write(html_content)

if __name__ == "__main__":
    main()
