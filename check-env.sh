#!/bin/bash

# 环境变量检查脚本
# 用于验证 GOOGLE_API_KEY 是否正确设置

echo "=========================================="
echo "  环境变量检查工具"
echo "=========================================="
echo ""

# 检查 GOOGLE_API_KEY 是否设置
if [ -z "$GOOGLE_API_KEY" ]; then
    echo "❌ GOOGLE_API_KEY 未设置"
    echo ""
    echo "请使用以下命令设置："
    echo "  export GOOGLE_API_KEY=\"your-google-api-key-here\""
    echo ""
    echo "获取 API Key："
    echo "  访问 https://aistudio.google.com/app/apikey"
    echo ""
    exit 1
else
    echo "✅ GOOGLE_API_KEY 已设置"
    echo ""
    echo "API Key 前缀: ${GOOGLE_API_KEY:0:15}..."
    echo "API Key 长度: ${#GOOGLE_API_KEY} 字符"
    echo ""
    
    # 检查 API Key 格式（Google API Key 通常以 AIza 开头）
    if [[ $GOOGLE_API_KEY == AIza* ]]; then
        echo "✅ API Key 格式看起来正确（以 AIza 开头）"
    else
        echo "⚠️  警告: API Key 格式可能不正确（通常以 AIza 开头）"
    fi
    echo ""
fi

# 检查 Shell 类型
echo "当前 Shell: $SHELL"
echo ""

# 检查配置文件
echo "配置文件检查："
if [ -f ~/.bash_profile ]; then
    if grep -q "GOOGLE_API_KEY" ~/.bash_profile; then
        echo "  ✅ ~/.bash_profile 包含 GOOGLE_API_KEY"
    else
        echo "  ⚪ ~/.bash_profile 不包含 GOOGLE_API_KEY"
    fi
fi

if [ -f ~/.bashrc ]; then
    if grep -q "GOOGLE_API_KEY" ~/.bashrc; then
        echo "  ✅ ~/.bashrc 包含 GOOGLE_API_KEY"
    else
        echo "  ⚪ ~/.bashrc 不包含 GOOGLE_API_KEY"
    fi
fi

if [ -f ~/.zshrc ]; then
    if grep -q "GOOGLE_API_KEY" ~/.zshrc; then
        echo "  ✅ ~/.zshrc 包含 GOOGLE_API_KEY"
    else
        echo "  ⚪ ~/.zshrc 不包含 GOOGLE_API_KEY"
    fi
fi

if [ -f .env ]; then
    if grep -q "GOOGLE_API_KEY" .env; then
        echo "  ✅ .env 包含 GOOGLE_API_KEY"
    else
        echo "  ⚪ .env 不包含 GOOGLE_API_KEY"
    fi
fi

echo ""
echo "=========================================="
echo "  检查完成"
echo "=========================================="
echo ""

if [ -z "$GOOGLE_API_KEY" ]; then
    echo "下一步: 设置环境变量"
    echo "  export GOOGLE_API_KEY=\"your-key\""
else
    echo "下一步: 运行测试"
    echo "  ./run-tests.sh --all"
    echo "  或"
    echo "  mvn test"
fi
echo ""

