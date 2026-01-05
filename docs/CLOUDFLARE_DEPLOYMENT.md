# Cloudflare Pages Deployment Setup

## Overview
This repository now includes automatic deployment to Cloudflare Pages when JavaScript, HTML, or CSS files are modified.

## Configuration Required

To enable automatic deployment, you need to configure the following secrets in your GitHub repository settings:

### 1. CLOUDFLARE_API_TOKEN
Create a Cloudflare API token with the following permissions:
- Account > Cloudflare Pages > Edit

To create the token:
1. Log in to the Cloudflare Dashboard
2. Go to "My Profile" > "API Tokens"
3. Click "Create Token"
4. Click "Create Custom Token"
5. Set the Token name (e.g., "GitHub Pages Deploy")
6. Under Permissions, add:
   - Account > Cloudflare Pages > Edit
7. Set Account Resources to include the account where your Pages project exists
8. Click "Continue to summary" and then "Create Token"
9. Copy the token and add it as a GitHub secret named `CLOUDFLARE_API_TOKEN`

### 2. CLOUDFLARE_ACCOUNT_ID
Your Cloudflare Account ID can be found:
1. Log in to the Cloudflare Dashboard
2. Select any site/domain
3. Look at the URL or check the sidebar - the Account ID is displayed there
4. Add it as a GitHub secret named `CLOUDFLARE_ACCOUNT_ID`

### 3. Cloudflare Pages Project
Make sure you have a Cloudflare Pages project created:
1. Go to the Cloudflare Dashboard
2. Navigate to "Workers & Pages" > "Pages"
3. Create a new project or use an existing one
4. Note the project name

**Option A: Set as Repository Variable (Recommended)**
1. Go to your GitHub repository Settings
2. Navigate to "Secrets and variables" > "Actions" > "Variables"
3. Add a new repository variable named `CLOUDFLARE_PROJECT_NAME`
4. Set its value to your Cloudflare Pages project name

**Option B: Update Workflow File**
Edit `.github/workflows/cloudflare-pages.yml` and update the `CLOUDFLARE_PROJECT_NAME` default value in the `env` section to match your project name.

### 4. Deployment Directory (Optional)
The workflow deploys the `.javadoc` directory by default. If you need to deploy a different directory:

**Option A: Set as Repository Variable**
1. Go to your GitHub repository Settings
2. Navigate to "Secrets and variables" > "Actions" > "Variables"
3. Add a new repository variable named `DEPLOYMENT_DIRECTORY`
4. Set its value to the directory you want to deploy (e.g., `docs`, `public`, or `.`)

**Option B: Update Workflow File**
Edit `.github/workflows/cloudflare-pages.yml` and update the `DEPLOYMENT_DIRECTORY` default value in the `env` section.

## How It Works

The workflow automatically triggers when:
- Changes are pushed to the `master` branch
- Modified files include: `**.js`, `**.html`, `**.css`, or files in `.javadoc/**`
- You can also manually trigger the deployment from the Actions tab

## Manual Deployment

To manually trigger a deployment:
1. Go to the "Actions" tab in GitHub
2. Select "Deploy to Cloudflare Pages" workflow
3. Click "Run workflow"
4. Select the branch and click "Run workflow"

## Customizing the Deployment

### Understanding triggers vs deployment directory
The workflow triggers when JS/HTML/CSS files change anywhere in the repository, but only deploys a specific directory (`.javadoc` by default). This is intentional:
- If your JS/HTML/CSS files are in the deployment directory, changes will trigger deployment of that directory
- If your source files are elsewhere and compiled/copied to the deployment directory, you should commit those compiled files or add a build step to the workflow

### Change the deployment directory
Edit `.github/workflows/cloudflare-pages.yml` and update the `directory` field under the Cloudflare Pages action step.

### Add build steps
If your documentation requires a build process before deployment, add the necessary build commands in the "Build documentation" step.

### Change trigger conditions
Modify the `paths` section to trigger on different file types or patterns.

## Troubleshooting

### Workflow fails with authentication errors
- Verify that `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` secrets are correctly set
- Ensure the API token has the correct permissions
- Check that the token hasn't expired

### Deployment succeeds but changes don't appear
- Check Cloudflare Pages dashboard to see if the deployment completed
- Cloudflare may cache pages - try clearing your browser cache
- Verify that the correct directory is being deployed

### Files not triggering deployment
- Check the `paths` filter in the workflow file
- Make sure the modified files match one of the patterns
- You can always manually trigger the workflow if needed
