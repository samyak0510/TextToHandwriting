# Contributing to TextToHandwriting

We love your input! We want to make contributing to TextToHandwriting as easy and transparent as possible, whether it's:

- Reporting a bug
- Discussing the current state of the code
- Submitting a fix
- Proposing new features
- Becoming a maintainer

## Development Process

We use GitHub to host code, track issues and feature requests, and accept pull requests.

### Pull Request Process

1. Fork the repo and create your branch from `main`.
2. If you've added code that should be tested, add tests.
3. If you've changed APIs, update the documentation.
4. Ensure the test suite passes.
5. Make sure your code lints.
6. Issue that pull request!

## Development Setup

### Prerequisites

- **Java 17+** for backend development
- **Android Studio** for mobile development
- **Python 3.8+** for testing and scripts
- **Git** for version control

### Quick Start

```bash
# Clone your fork
git clone https://github.com/samyak0510/TextToHandwriting.git
cd TextToHandwriting

# Setup backend
cd src/backend
mvn clean install
mvn spring-boot:run

# Setup Android (in separate terminal)
# Open src/android/ in Android Studio
# Sync project and run on device/emulator


## Code Standards

### Java (Backend & Android)

- **Style**: Follow Google Java Style Guide
- **Documentation**: Use JavaDoc for all public methods

```java
/**
 * Professional method documentation with complete parameter and return descriptions.
 * 
 * @param inputParameter Description of what this parameter does
 * @return Description of what this method returns
 * @throws ExceptionType When this exception might be thrown
 */
public ReturnType methodName(ParameterType inputParameter) {
    logger.info("Starting operation: {}", operationName);
    // Implementation
}
```

## Git Workflow

### Branch Naming

- `feature/description` - New features
- `bugfix/description` - Bug fixes
- `hotfix/description` - Critical fixes
- `docs/description` - Documentation updates

### Commit Messages

Follow the [Conventional Commits](https://conventionalcommits.org/) specification:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

Examples:
```
feat(backend): add automatic tool path detection
fix(android): resolve network configuration issue
docs(api): update endpoint documentation
test(integration): add font processing pipeline tests
```

### Commit Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding missing tests
- `chore`: Build process or auxiliary tool changes

## Documentation Standards

### Code Documentation

- **Public APIs**: Full JavaDoc/docstring documentation
- **Complex Logic**: Inline comments explaining why, not what
- **Configuration**: Document all configuration options
- **Examples**: Provide usage examples in documentation

### README Updates

When adding features, update:
- Installation instructions
- Configuration examples
- Usage examples
- API documentation references

## Performance Guidelines

### Backend

- **Async Processing**: Use `@Async` for long-running operations
- **Resource Management**: Proper cleanup of temporary files
- **Error Handling**: Comprehensive exception handling
- **Logging**: Appropriate log levels and structured logging

### Android

- **Memory Management**: Avoid memory leaks
- **Background Tasks**: Use appropriate threading
- **Network Efficiency**: Implement proper timeouts and retry logic
- **UI Responsiveness**: Keep UI operations on main thread minimal

## Security Guidelines

### API Keys

- **Never commit secrets**: Use Android Keystore for sensitive data
- **Environment Variables**: Use env vars for configuration
- **Validation**: Validate all inputs thoroughly


## Issue Reporting

### Bug Reports

Include:
- **Environment**: OS, device, versions
- **Steps to Reproduce**: Detailed reproduction steps
- **Expected Behavior**: What should happen
- **Actual Behavior**: What actually happens
- **Logs**: Relevant log output
- **Screenshots**: If applicable

### Feature Requests

Include:
- **Problem Statement**: What problem does this solve?
- **Proposed Solution**: How should it work?
- **Alternatives Considered**: Other approaches you've thought about
- **Additional Context**: Any other relevant information

## Code Review Guidelines

### For Authors

- **Small PRs**: Keep changes focused and small
- **Self Review**: Review your own code first
- **Documentation**: Update docs if needed

### For Reviewers

- **Be Constructive**: Provide helpful, actionable feedback
- **Ask Questions**: Don't hesitate to ask for clarification
- **Consider Performance**: Look for potential performance issues

## Communication

### Getting Help

- **GitHub Issues**: For bugs and feature requests
- **GitHub Discussions**: For questions and general discussion
- **Code Comments**: For specific implementation questions

### Stay Updated

- Watch the repository for notifications
- Read the changelog for each release
- Follow coding standards and best practices

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (MIT License).

## Recognition

Contributors will be acknowledged in:
- README.md contributor section
- Release notes for significant contributions
- Code comments for substantial improvements

---

Thank you for contributing <3