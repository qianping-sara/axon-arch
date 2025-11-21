# ATAM Copilot

AI-assisted Architecture Tradeoff Analysis Method (ATAM) evaluation platform.

## 📋 Project Overview

ATAM Copilot is an intelligent architecture decision support platform that leverages generative AI to assist architects in executing the ATAM process, identifying architecture risks and tradeoff points.

### Key Features

- 🤖 **AI-Assisted Analysis**: Automated extraction of business drivers, architecture patterns, and risks
- 🏗️ **Agentic Architecture**: Four-layer model (L3: Orchestration, L2: Agents, L1: Tools, L0: Persistence)
- 🔄 **Human-in-the-Loop**: AI generates drafts, humans review and refine
- 📊 **Structured Outputs**: Standardized ATAM artifacts with JSON schema validation
- 🔌 **Extensible**: Easy to add new agents and tools

---

## ⚠️ IMPORTANT: This Repository Contains ONLY Source Code

**This Git repository tracks ONLY application source code and configuration files.**

- 📐 **Design Documents** → `../Design_doc/` (NOT in Git - maintained separately)
- 📝 **Summary Documents** → `../Summary_doc/` (NOT in Git - maintained separately)
- 💻 **Source Code** → This repository (tracked in Git)

**This README is the ONLY documentation file in this repository.**

**See `CONTRIBUTING.md` for detailed guidelines.**

---

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- (Optional) PostgreSQL 14+ for production
- (Optional) OpenAI API key for AI features

### Running the Application

```bash
# Clone the repository
cd atam-copilot

# Run with Maven
mvn spring-boot:run

# Or build and run the JAR
mvn clean package
java -jar target/atam-copilot-1.0.0-SNAPSHOT.jar
```

### Access the Application

- **Health Check**: http://localhost:8080/api/v1/health
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console** (dev only): http://localhost:8080/h2-console
- **Actuator Health**: http://localhost:8080/actuator/health

## 🏗️ Architecture

### Agentic Four-Layer Model

```
┌─────────────────────────────────────────┐
│  L3: Orchestration Layer                │
│  - ATAM Process Orchestration           │
│  - State Management                     │
├─────────────────────────────────────────┤
│  L2: Domain Intelligence Layer          │
│  - Business Driver Agent                │
│  - Architecture Design Agent            │
│  - Risk Analysis Agent                  │
├─────────────────────────────────────────┤
│  L1: Deterministic Services Layer       │
│  - Document Parser Tool                 │
│  - Persistence Tool                     │
│  - Schema Validator Service             │
│  - Report Generator Tool                │
├─────────────────────────────────────────┤
│  L0: Persistence Layer                  │
│  - JPA Repositories                     │
│  - Database Access                      │
└─────────────────────────────────────────┘
```

### Project Structure

```
atam-copilot/
├── src/main/java/com/atam/
│   ├── config/              # Configuration classes
│   ├── orchestration/       # L3: Process control
│   ├── agents/              # L2: AI agents
│   │   ├── base/           # Agent infrastructure
│   │   ├── business/       # Business Driver Agent
│   │   ├── architecture/   # Architecture Design Agent
│   │   └── risk/           # Risk Analysis Agent
│   ├── tools/              # L1: Deterministic tools
│   ├── domain/             # Domain models
│   ├── repository/         # L0: Data access
│   ├── service/            # Business services
│   ├── controller/         # REST controllers
│   ├── dto/                # Data transfer objects
│   ├── exception/          # Exception handling
│   └── common/             # Common utilities
├── src/main/resources/
│   ├── prompts/            # Prompt templates
│   ├── schemas/            # JSON schemas
│   └── knowledge/          # Knowledge base
└── src/test/               # Test classes
```

## 🛠️ Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **AI Framework**: Spring AI 1.1.0
- **AI Model**: Google Gemini 2.5 Flash (via Gemini Developer API / Vertex AI)
- **AI SDK**: Google GenAI Java SDK 1.28.0
- **Database**: PostgreSQL (prod) / H2 (dev)
- **Build Tool**: Maven
- **API Documentation**: SpringDoc OpenAPI (Swagger)
- **Document Processing**: Gemini Files API (PDF, up to 50MB or 1,000 pages)

## 📝 Development

### Configuration

The application supports multiple profiles:

- `dev` (default): Uses H2 in-memory database
- `prod`: Uses PostgreSQL database

Set the active profile:
```bash
export SPRING_PROFILES_ACTIVE=dev
```

### Environment Variables

For AI features, set your Google API key:
```bash
export GOOGLE_API_KEY=your-api-key-here
```

## 📖 API Usage

### Business Driver Extraction

The platform provides two approaches for extracting business drivers from PDF documents:

#### Recommended Approach: Decoupled Upload and Extraction

This approach allows file reuse across multiple agents and provides better control over the file lifecycle.

**Step 1: Upload PDF Files**

```bash
curl -X POST http://localhost:8080/api/v1/files/upload \
  -F "files=@architecture-doc.pdf" \
  -F "files=@requirements.pdf"
```

Response:
```json
[
  {
    "fileId": "files/abc123",
    "uri": "https://generativelanguage.googleapis.com/v1beta/files/abc123",
    "displayName": "architecture-doc.pdf",
    "sizeBytes": 1024000,
    "mimeType": "application/pdf",
    "state": "ACTIVE"
  }
]
```

**Step 2: Extract Business Drivers (Streaming)**

```bash
curl -X POST http://localhost:8080/api/v1/business-drivers/extract/stream \
  -H "Content-Type: application/json" \
  -d '{
    "fileUris": [
      "https://generativelanguage.googleapis.com/v1beta/files/abc123"
    ]
  }'
```

**Step 2 (Alternative): Extract Business Drivers (Synchronous)**

```bash
curl -X POST http://localhost:8080/api/v1/business-drivers/extract \
  -H "Content-Type: application/json" \
  -d '{
    "fileUris": [
      "https://generativelanguage.googleapis.com/v1beta/files/abc123"
    ]
  }'
```

**Benefits**:
- ✅ Upload files once, use with multiple agents (Business Driver, Architecture Design, Risk Analysis)
- ✅ Files retained on Gemini servers for 48 hours
- ✅ Frontend controls file lifecycle
- ✅ Better separation of concerns

#### Legacy Approach: Upload and Extract in One Step (Deprecated)

For backward compatibility, you can still upload and extract in a single request:

```bash
# Streaming
curl -X POST http://localhost:8080/api/v1/business-drivers/extract/stream/upload \
  -F "files=@architecture-doc.pdf"

# Synchronous
curl -X POST http://localhost:8080/api/v1/business-drivers/extract/upload \
  -F "files=@architecture-doc.pdf"
```

**Note**: This approach is deprecated and will be removed in future versions. Please migrate to the decoupled approach.

### File Reuse Example

```bash
# 1. Upload once
UPLOAD_RESPONSE=$(curl -X POST http://localhost:8080/api/v1/files/upload \
  -F "files=@architecture-doc.pdf")

# 2. Extract file URI
FILE_URI=$(echo $UPLOAD_RESPONSE | jq -r '.[0].uri')

# 3. Use with Business Driver Agent
curl -X POST http://localhost:8080/api/v1/business-drivers/extract/stream \
  -H "Content-Type: application/json" \
  -d "{\"fileUris\": [\"$FILE_URI\"]}"

# 4. Reuse with Architecture Design Agent (future)
curl -X POST http://localhost:8080/api/v1/architecture/analyze/stream \
  -H "Content-Type: application/json" \
  -d "{\"fileUris\": [\"$FILE_URI\"]}"
```

## 📖 API Documentation

Once the application is running, visit:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report
```

## 📦 Building

```bash
# Build JAR
mvn clean package

# Skip tests
mvn clean package -DskipTests
```

## 🤝 Contributing

Please refer to the design documents in `Design_doc/` for architecture and implementation guidelines.

## 📄 License

Apache 2.0

## 👥 Team

ATAM Copilot Team

---

**Version**: 1.0.0
**Last Updated**: 2025-11-21

