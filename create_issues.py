import os
import json
from github import Github

# === CONFIGURATION ===
# Read the GitHub token from the environment variable.
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")
if not GITHUB_TOKEN:
    raise Exception("Please set the GITHUB_TOKEN environment variable.")

# Update REPO_NAME with the full repository identifier (e.g., "team-org/project-repo")
REPO_NAME = "minnaheim/group_19_server"
USER_STORIES_FILE = "changed_issues.json"
# =====================

def load_user_stories(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        return json.load(f)

def create_github_issue(repo, story):
    # Construct title in the "role-goal-benefit" format.
    title = f"As a {story['role']}, I want to {story['goal']} so that I can {story['benefit']}."
    
    # Construct a Markdown task list for the acceptance criteria.
    description = "### Acceptance Criteria\n"
    for criterion in story["acceptance_criteria"]:
        description += f"- [ ] {criterion}\n"
    
    # Optionally, you could append the time estimate if needed.
    description += f"\n**Time Estimate:** {story.get('time_estimate', 'N/A')}\n"
    
    # Create the issue on GitHub.
    issue = repo.create_issue(title=title, body=description)
    return issue

def main():
    # Initialize GitHub connection using the token.
    g = Github(GITHUB_TOKEN)
    repo = g.get_repo(REPO_NAME)
    
    # Load all user stories from the JSON file.
    stories = load_user_stories(USER_STORIES_FILE)
    
    # Create an issue for each user story.
    for story in stories:
        issue = create_github_issue(repo, story)
        print(f"Issue created: {issue.title}")

if __name__ == "__main__":
    main()
