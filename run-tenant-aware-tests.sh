#!/bin/bash

# Test runner script for TenantAwareDataSourceTest
# This script helps verify the test suite

echo "========================================"
echo "TenantAwareDataSourceTest Verification"
echo "========================================"
echo ""

# Check if Maven is available
if command -v mvn &> /dev/null; then
    echo "✓ Maven found"
    echo ""
    echo "Running TenantAwareDataSourceTest..."
    mvn test -Dtest=TenantAwareDataSourceTest -q
    TEST_RESULT=$?
    
    if [ $TEST_RESULT -eq 0 ]; then
        echo ""
        echo "✓ All tests passed!"
    else
        echo ""
        echo "✗ Some tests failed. Check output above."
    fi
    exit $TEST_RESULT
else
    echo "✗ Maven not found"
    echo ""
    echo "To run the tests, install Maven and execute:"
    echo "  mvn test -Dtest=TenantAwareDataSourceTest"
    echo ""
    echo "Or use your IDE's test runner (IntelliJ IDEA, Eclipse, VS Code)"
    exit 1
fi
