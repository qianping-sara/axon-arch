# Contributing Guidelines

## 📁 Document Organization Rules

### **IMPORTANT: This Repository Contains ONLY Source Code**

This Git repository (`atam-copilot/`) contains **ONLY application source code and configuration files**.

**Design and summary documents are NOT tracked in Git** - they are maintained separately in the parent directory.

---

## ✅ What SHOULD Be in This Repository

### Source Code & Configuration
- ✅ Java source code (`src/main/java/**/*.java`)
- ✅ Test code (`src/test/java/**/*.java`)
- ✅ Configuration files (`src/main/resources/**/*.yml`, `*.properties`)
- ✅ Maven configuration (`pom.xml`)
- ✅ Git configuration (`.gitignore`)
- ✅ **ONLY ONE** documentation file: `README.md` (project overview)

---

## ❌ What Should NOT Be in This Repository

### Documentation Files (Managed Outside Git)
- ❌ Design documents (maintained in `../Design_doc/`)
- ❌ Summary documents (maintained in `../Summary_doc/`)
- ❌ Architecture diagrams (unless in `src/main/resources/diagrams/`)
- ❌ Meeting notes
- ❌ Planning documents
- ❌ Setup guides
- ❌ Any `.md` files except `README.md`

### Examples of Forbidden Files
```
❌ DESIGN.md
❌ ARCHITECTURE.md
❌ PROJECT_SETUP.md
❌ MEETING_NOTES.md
❌ TASK_BREAKDOWN.md
❌ API_SPECIFICATION.md
❌ SUMMARY.md
```

---

## 📂 Directory Structure

### Inside Git Repository (`atam-copilot/`)
```
atam-copilot/                    # ← Git repository root
├── .git/                        # Git metadata
├── .gitignore                   # Git ignore rules
├── README.md                    # ✅ ONLY documentation file allowed
├── CONTRIBUTING.md              # ✅ Contribution guidelines
├── pom.xml                      # Maven configuration
└── src/
    ├── main/
    │   ├── java/                # Java source code
    │   └── resources/           # Configuration files
    └── test/
        └── java/                # Test code
```

### Outside Git Repository (Parent Directory)
```
axon-arch/                       # ← NOT in Git
├── Design_doc/                  # Design documents (NOT in Git)
│   ├── ATAM.md
│   ├── Phase1_Project_Structure_Design.md
│   └── Spring_AI_Framework_Learning_Report.md
│
├── Summary_doc/                 # Summary documents (NOT in Git)
│   └── PROJECT_SETUP.md
│
└── atam-copilot/                # ← Git repository (THIS directory)
    └── (source code only)
```

---

## 🚫 Pre-Commit Checklist

Before committing, verify:

- [ ] **No design documents** in the repository
- [ ] **No summary documents** in the repository
- [ ] **Only `README.md`** exists (no other `.md` files)
- [ ] All changes are **source code or configuration**
- [ ] `.gitignore` is properly configured

---

## 📝 How to Add Documentation

### If You Need to Add Design Documentation
1. **DO NOT** add it to this Git repository
2. Add it to `../Design_doc/` (parent directory)
3. Update `../Design_doc/` index if needed

### If You Need to Add Summary Documentation
1. **DO NOT** add it to this Git repository
2. Add it to `../Summary_doc/` (parent directory)
3. Update `../Summary_doc/` index if needed

### If You Need to Update Project Overview
1. Edit `README.md` (the ONLY allowed documentation file)
2. Keep it concise - detailed docs go in parent directories

---

## 🔍 Verification Script

Run this before committing:

```bash
# Check for forbidden documentation files
find . -maxdepth 1 -name "*.md" ! -name "README.md" ! -name "CONTRIBUTING.md"

# Should return nothing - if it finds files, they should be removed
```

---

## 🤝 Code Contribution Guidelines

### Code Style
- Follow Java coding conventions
- Use meaningful variable and method names
- Add JavaDoc for public APIs
- Keep methods focused and small

### Testing
- Write unit tests for new features
- Ensure all tests pass before committing
- Aim for high code coverage

### Commit Messages
- Use clear, descriptive commit messages
- Format: `[Type] Brief description`
- Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`

Example:
```
feat: Add BusinessDriverAgent implementation
fix: Resolve null pointer in DocumentParserTool
refactor: Simplify ArchitecturePatternDetector logic
test: Add unit tests for RiskAnalysisAgent
```

---

## 📋 Pull Request Process

1. Create a feature branch from `main`
2. Make your changes (code only, no docs)
3. Write/update tests
4. Ensure all tests pass
5. Verify no documentation files are included
6. Submit pull request with clear description

---

**Last Updated**: 2025-11-19  
**Maintained by**: ATAM Copilot Team

