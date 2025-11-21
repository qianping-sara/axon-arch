#!/bin/bash

# ATAM Copilot Test Runner
# Version: 1.0
# Date: 2025-11-20

set -e

echo "=========================================="
echo "ATAM Copilot Test Runner"
echo "=========================================="
echo ""

# Check if GOOGLE_API_KEY is set
if [ -z "$GOOGLE_API_KEY" ]; then
    echo "❌ Error: GOOGLE_API_KEY environment variable is not set"
    echo ""
    echo "Please set your Google API Key:"
    echo "  export GOOGLE_API_KEY=\"your-api-key-here\""
    echo ""
    echo "Get your API Key from: https://aistudio.google.com/app/apikey"
    exit 1
fi

echo "✅ GOOGLE_API_KEY is set"
echo ""

# Parse command line arguments
TEST_CLASS=""
TEST_METHOD=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --class)
            TEST_CLASS="$2"
            shift 2
            ;;
        --method)
            TEST_METHOD="$2"
            shift 2
            ;;
        --all)
            TEST_CLASS=""
            TEST_METHOD=""
            shift
            ;;
        --help)
            echo "Usage: ./run-tests.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --all              Run all tests (default)"
            echo "  --class <name>     Run specific test class"
            echo "  --method <name>    Run specific test method (requires --class)"
            echo "  --help             Show this help message"
            echo ""
            echo "Examples:"
            echo "  ./run-tests.sh --all"
            echo "  ./run-tests.sh --class GeminiChatServiceTest"
            echo "  ./run-tests.sh --class BusinessDriverAgentTest --method testExtractBusinessDriversWithRealPdf"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Build test command
if [ -n "$TEST_CLASS" ]; then
    if [ -n "$TEST_METHOD" ]; then
        echo "🧪 Running test: $TEST_CLASS#$TEST_METHOD"
        TEST_CMD="mvn test -Dtest=$TEST_CLASS#$TEST_METHOD"
    else
        echo "🧪 Running test class: $TEST_CLASS"
        TEST_CMD="mvn test -Dtest=$TEST_CLASS"
    fi
else
    echo "🧪 Running all tests"
    TEST_CMD="mvn test"
fi

echo ""
echo "=========================================="
echo "Executing: $TEST_CMD"
echo "=========================================="
echo ""

# Run tests
$TEST_CMD

# Check exit code
if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✅ Tests completed successfully!"
    echo "=========================================="
else
    echo ""
    echo "=========================================="
    echo "❌ Tests failed!"
    echo "=========================================="
    exit 1
fi

