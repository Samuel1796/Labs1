# Git Workflow Documentation

## Recommended Workflow

1. **Clone the Repository**
    ```bash
    git clone <repository-url>
    cd Labs1
    ```

2. **Create a New Branch**
    ```bash
    git checkout -b feature/your-feature-name
    ```

3. **Make Changes**
    - Edit code, add tests, update documentation.

4. **Commit Changes**
    ```bash
    git add .
    git commit -m "Describe your changes"
    ```

5. **Push to Remote**
    ```bash
    git push origin feature/your-feature-name
    ```

6. **Create a Pull Request**
    - Go to your Git hosting service (GitHub, GitLab, etc.).
    - Open a pull request for review.

7. **Merge After Review**
    - Once approved, merge your branch into `main` or `master`.

## Best Practices

- Commit often with clear messages.
- Pull latest changes before starting new work.
- Write tests for new features.
- Keep branches focused (one feature or fix per branch).
- Delete branches after merging.

## Useful Commands

- `git status` — Check current changes.
- `git log` — View commit history.
- `git pull` — Fetch and merge latest changes.
- `git fetch` — Fetch latest changes without merging.