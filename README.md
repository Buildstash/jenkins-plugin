# Buildstash Jenkins Plugin

A Jenkins plugin for uploading build artifacts to the [Buildstash](https://buildstash.com) web service. This plugin provides a pipeline step that can be used in Jenkins pipelines to upload files to Buildstash, supporting both direct file uploads and chunked uploads for large files.

## Features

- **Pipeline Step**: Easy-to-use pipeline step for uploading build artifacts
- **Freestyle Support**: Build step for classic Jenkins Freestyle projects
- **Chunked Uploads**: Support for large file uploads using chunked multipart uploads
- **Direct Uploads**: Simple direct file uploads for smaller files
- **File + Expansion**: Support for uploading primary files with expansion files
- **Version Control Integration**: Automatic integration with Git repositories
- **CI/CD Metadata**: Comprehensive build metadata collection
- **Multiple Platforms**: Support for various platforms (iOS, Android, etc.)

## Installation

### Prerequisites

- Jenkins 2.387.3 or later
- Java 11 or later

### Manual Installation

1. Build the plugin:
   ```bash
   mvn clean package
   ```

2. Install the generated `.hpi` file in Jenkins:
   - Go to **Manage Jenkins** > **Manage Plugins** > **Advanced**
   - Upload the `.hpi` file in the **Upload Plugin** section
   - Restart Jenkins

### Development Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/jenkinsci/buildstash-plugin.git
   cd buildstash-plugin
   ```

2. Run Jenkins with the plugin:
   ```bash
   mvn hpi:run
   ```

## Usage

### Pipeline Step

The plugin provides a `buildstash` step that can be used in Jenkins pipelines:

```groovy
pipeline {
    agent any
    
    stages {
        stage('Build') {
            steps {
                // Your build steps here
                sh 'make build'
            }
        }
        
        stage('Upload to Buildstash') {
            steps {
                buildstash(
                    apiKey: 'your-api-key',
                    structure: 'file',
                    primaryFilePath: 'build/app.ipa',
                    versionComponent1Major: '1',
                    versionComponent2Minor: '0',
                    versionComponent3Patch: '0',
                    platform: 'ios',
                    stream: 'development',
                    labels: 'beta\ntest',
                    architectures: 'arm64\nx86_64'
                )
            }
        }
    }
}
```

### Freestyle Project

The plugin also provides a build step for classic Jenkins Freestyle projects:

1. **Create a Freestyle Project**: Go to Jenkins and create a new Freestyle project
2. **Add Build Steps**: Configure your build steps (compile, test, etc.)
3. **Add Buildstash Step**: Add a new build step and select "Upload to Buildstash"
4. **Configure Parameters**: Fill in the required parameters:
   - API Key
   - Primary File Path
   - Version components (Major, Minor, Patch)
   - Platform
   - Stream
5. **Save and Run**: Save the project configuration and run the build

The build step will appear in the "Build" section of your Freestyle project configuration, and you can configure all the same parameters as the pipeline step.

After a successful upload, the build results will be displayed on the build page with links to:
- Build ID
- Build Info URL
- Download URL
- Processing status

### File + Expansion Upload

For platforms that require both a primary file and an expansion file:

```groovy
buildstash(
    apiKey: 'your-api-key',
    structure: 'file+expansion',
    primaryFilePath: 'build/app.aab',
    expansionFilePath: 'build/app-obb.zip',
    versionComponent1Major: '1',
    versionComponent2Minor: '0',
    versionComponent3Patch: '0',
    platform: 'android',
    stream: 'production'
)
```

### Advanced Configuration

```groovy
buildstash(
    apiKey: 'your-api-key',
    structure: 'file',
    primaryFilePath: 'build/app.ipa',
    versionComponent1Major: '1',
    versionComponent2Minor: '0',
    versionComponent3Patch: '0',
    versionComponentExtra: 'beta',
    versionComponentMeta: 'build.123',
    customBuildNumber: '2023.12.01',
    labels: 'beta\ntest\ninternal',
    architectures: 'arm64',
    ciPipeline: 'Jenkins Pipeline',
    ciRunId: env.BUILD_NUMBER,
    ciRunUrl: env.BUILD_URL,
    ciBuildDuration: currentBuild.duration.toString(),
    vcHostType: 'git',
    vcHost: 'github',
    vcRepoName: 'my-app',
    vcRepoUrl: 'https://github.com/user/my-app',
    vcBranch: env.GIT_BRANCH,
    vcCommitSha: env.GIT_COMMIT,
    vcCommitUrl: "https://github.com/user/my-app/commit/${env.GIT_COMMIT}",
    platform: 'ios',
    stream: 'development',
    notes: 'Built with Jenkins on macOS'
)
```

## Parameters

### Required Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `apiKey` | Your Buildstash API key | `'your-api-key'` |
| `primaryFilePath` | Path to the primary file to upload | `'build/app.ipa'` |
| `versionComponent1Major` | Major version component | `'1'` |
| `versionComponent2Minor` | Minor version component | `'0'` |
| `versionComponent3Patch` | Patch version component | `'0'` |
| `platform` | Target platform | `'ios'`, `'android'`, etc. |
| `stream` | Build stream | `'development'`, `'production'`, etc. |

### Optional Parameters

| Parameter | Description | Default | Example |
|-----------|-------------|---------|---------|
| `structure` | Upload structure type | `'file'` | `'file'`, `'file+expansion'` |
| `expansionFilePath` | Path to expansion file | `null` | `'build/app-obb.zip'` |
| `versionComponentExtra` | Extra version component | `null` | `'beta'` |
| `versionComponentMeta` | Meta version component | `null` | `'build.123'` |
| `customBuildNumber` | Custom build number | `null` | `'2023.12.01'` |
| `labels` | Labels (newline-separated) | `null` | `'beta\ntest'` |
| `architectures` | Architectures (newline-separated) | `null` | `'arm64\nx86_64'` |
| `ciPipeline` | CI pipeline name | `null` | `'Jenkins Pipeline'` |
| `ciRunId` | CI run ID | `null` | `env.BUILD_NUMBER` |
| `ciRunUrl` | CI run URL | `null` | `env.BUILD_URL` |
| `ciBuildDuration` | CI build duration | `null` | `currentBuild.duration.toString()` |
| `vcHostType` | Version control host type | `'git'` | `'git'` |
| `vcHost` | Version control host | `'github'` | `'github'`, `'gitlab'` |
| `vcRepoName` | Repository name | `null` | `'my-app'` |
| `vcRepoUrl` | Repository URL | `null` | `'https://github.com/user/my-app'` |
| `vcBranch` | Branch name | `null` | `env.GIT_BRANCH` |
| `vcCommitSha` | Commit SHA | `null` | `env.GIT_COMMIT` |
| `vcCommitUrl` | Commit URL | `null` | `'https://github.com/user/my-app/commit/abc123'` |
| `notes` | Build notes | `null` | `'Built with Jenkins'` |

## Outputs

The step provides the following outputs that can be accessed in subsequent pipeline steps:

- `buildId`: The ID of the uploaded build
- `pendingProcessing`: Whether the build is pending additional processing
- `buildInfoUrl`: URL to view the build information on Buildstash
- `downloadUrl`: URL to download the build

Example usage of outputs:

```groovy
def result = buildstash(
    apiKey: 'your-api-key',
    // ... other parameters
)

echo "Build ID: ${result.buildId}"
echo "Build Info URL: ${result.buildInfoUrl}"
echo "Download URL: ${result.downloadUrl}"
```

## Supported Platforms

The plugin supports various platforms including:

- **iOS**: `.ipa`, `.app` files
- **Android**: `.apk`, `.aab` files
- **macOS**: `.dmg`, `.pkg` files
- **Windows**: `.exe`, `.msi` files
- **Linux**: Various package formats
- **Web**: Web application bundles

## File Size Limits

- **Direct Upload**: Files up to 5GB
- **Chunked Upload**: Files larger than 5GB (automatically handled)

## Error Handling

The plugin provides comprehensive error handling:

- **File Not Found**: Validates that specified files exist before upload
- **Network Errors**: Retries failed uploads with exponential backoff
- **API Errors**: Detailed error messages from the Buildstash API
- **Validation Errors**: Parameter validation with helpful error messages

## Security

- API keys are masked in Jenkins logs
- HTTPS communication with Buildstash API
- No sensitive data is stored in Jenkins

## Development

### Building

```bash
mvn clean package
```

### Testing

```bash
mvn test
```

### Running Tests with Jenkins

```bash
mvn hpi:run
```

### Code Style

The project follows standard Java conventions and uses:

- Java 11
- Maven for build management
- JUnit for testing
- Jenkins plugin parent POM

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support and questions:

- **Documentation**: [Buildstash Documentation](https://docs.buildstash.com)
- **Issues**: [GitHub Issues](https://github.com/jenkinsci/buildstash-plugin/issues)
- **Email**: support@buildstash.com

## Changelog

### Version 1.0.0
- Initial release
- Support for direct and chunked file uploads
- Pipeline step integration
- Comprehensive metadata collection
- Version control integration