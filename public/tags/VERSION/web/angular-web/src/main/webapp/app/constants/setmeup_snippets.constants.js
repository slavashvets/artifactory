let snippets = {
    debian: {
        read: [{
            before: "To use Artifactory repository to install Debian package you need to add it to your <i>source.list</i> file. You can do that using the following command:",
            snippet: "sudo sh -c \"echo 'deb $2/$1 <DISTRIBUTION> <COMPONENT>' >> /etc/apt/sources.list\""
        }, {
            before: "For accessing Artifactory using credentials you can specify it in the <i>source.list</i> file like so:",
            snippet: "http://<USERNAME>:<PASSWORD>@$4/artifactory/$1 <DISTRIBUTION> <COMPONENTS>"
        }, {
            before: "Your apt-get client will use the specified Artifactory repositories to install the package",
            snippet: "apt-get install <PACKAGE>"
        }],
        deploy: [{
            before: "To deploy a Debian package into Artifactory you can either use the deploy option in the Artifact’s module or upload with cURL using matrix parameters. The required parameters are package name, distribution, component, and architecture in the following way:",
            snippet: "curl <CURL_AUTH> -XPUT \"$2/$1/pool/<DEBIAN_PACKAGE_NAME>;deb.distribution=<DISTRIBUTION>;deb.component=<COMPONENT>;deb.architecture=<ARCHITECTURE>\" -T <PATH_TO_FILE>"
        }, {
            before: "You can specify multiple layouts by adding semicolon separated multiple parameters, like so:",
            snippet: "curl <CURL_AUTH> -XPUT \"$2/$1/pool/<DEBIAN_PACKAGE_NAME>;deb.distribution=<DISTRIBUTION>;deb.distribution=<DISTRIBUTION>;deb.component=<COMPONENT>;deb.component=<COMPONENT>;deb.architecture=<ARCHITECTURE>;deb.architecture=<ARCHITECTURE>\" -T <PATH_TO_FILE>",
            after: "To add an architecture independent layout use deb.architecture=all. This will cause your package to appear in the Packages index of all the architectures under the same Distribution and Component, as well as under a new index branch called binary-all which holds all Debian packages that are marked as \"all\"."
        }]
    },
    opkg: {
        read: [{
            before: "To use the Artifactory repository to install Ipk packages you need to add an indexed path (a feed) to your <i>opkg.conf</i> file. You can do that using the following command:",
            snippet: "echo 'src <FEED_NAME> http://$4/artifactory/$1/<PATH_TO_FEED>' >> /etc/opkg/opkg.conf",
            after: "If you want your client to download the .gz variant of the Packages index file instead, change the src part to src/gz"
        }, {
            before: "For accessing Artifactory using credentials you can specify it in the <i>opkg.conf</i> file like so:",
            snippet: "echo 'option http_auth <USERNAME>:<PASSWORD>' >> /etc/opkg/opkg.conf"
        }, {
            before: "Your Opkg client will use the specified Artifactory repositories to install the package",
            snippet: "opkg install <PACKAGE>"
        }],
        deploy: [{
            before: "To deploy an ipk package into Artifactory, run the following:",
            snippet: "curl <CURL_AUTH> -XPUT \"http://$4/artifactory/$1/<PATH_TO_FEED>/<IPK_PACKAGE_NAME>\" -T <PATH_TO_FILE>"
        }]
    },
    pypi: {
        read: [{
            before: "To resolve packages using pip, add the following to ~/.pip/pip.conf:",
            snippet: "[global]\nindex-url = $2/api/pypi/$1/simple"
        }, {
            before: "If credentials are required they should be embedded in the URL. To resolve packages using pip, run:",
            snippet: "pip install <PACKAGE>"
        }],
        deploy: [{
            before: "To deploy packages using setuptools you need to add an Artifactory repository to the <i>.pypirc</i> file (usually located in your home directory):",
            snippet: "[distutils]\n" + "index-servers = local\n" + "[local]\n" + "repository: $2/api/pypi/$1\n" + "username: <USERNAME>\n" + "password: <PASSWORD>"
        }, {
            before: "To deploy a python egg to Artifactory, after changing the <i>.pypirc</i> file, run the following command:",
            snippet: "python setup.py sdist upload -r local",
            after: "where &lt;local&gt; is the index server you defined in <i>.pypirc</i>."
        }]
    },
    bower: {
        general: [{
            before: "The following instructions apply to <b>Bower version 1.5</b> and above. For older versions, please refer to <a href=\"http://www.jfrog.com/confluence/display/RTF/Bower+Repositories#BowerRepositories-UsingOlderVersionsofBower\" target=\"_blank\">these instructions</a>."
        }, {
            before: "In order to use Bower with Artifactory you will need to add 'bower-art-resolver' as one of the resolvers in your .bowerrc file. To install <a href=\"https://www.google.com/url?q=https%3A%2F%2Fwww.npmjs.com%2Fpackage%2Fbower-art-resolver&sa=D&sntz=1&usg=AFQjCNH5pnW2E2ETaXtmJL33xBhGkxKPag\" target=\"_blank\">bower-art-resolver</a> (custom Bower resolver dedicated to integrate with Artifactory), run the following command:",
            snippet: "npm install -g bower-art-resolver"
        }, {
            before: "And add the bower-art-resolver as one of the resolvers in your <i>.bowerrc</i> file:",
            snippet: "{\n\t\"resolvers\" : [\n\t\t\"bower-art-resolver\"\n\t]\n}"
        },{
            before: "Now replace the default Bower registry with the following in your <i>.bowerrc</i> file:",
            snippet: "{\n\t\"registry\" : \"$2/api/bower/$1\",\n\t\"resolvers\" : [\n\t\t\"bower-art-resolver\"\n\t]\n}"
        }, {
            before: "If authentication is required use:",
            snippet: "{\n\t\"registry\" : \"http://<USERNAME>:<PASSWORD>@$4/artifactory/api/bower/$1\",\n\t\"resolvers\" : [\n\t\t\"bower-art-resolver\"\n\t]\n}"
        }],
        read: {
            before: "To install bower packages execute the following command:",
            snippet: "bower install <PACKAGE>"
        },
        deploy: {
            before: "To deploy a Bower package into an Artifactory repository you need to use Artifactory's REST API or the Web UI.<br/>For example, to deploy a Bower package into this repository using the REST API, use the following command:",
            snippet: "curl <CURL_AUTH> -XPUT $2/$1/<TARGET_FILE_PATH> -T <PATH_TO_FILE>"
        }
    },
    cocoapods: {
        general: [{
            before: "In order to use CocoaPods with Artifactory you will need to install the <a href=\"https://github.com/JFrogDev/cocoapods-art\" target=\"_blank\">'cocoapods-art'</a>. plugin. To install cocoapods-art run the following command:",
            snippet: "gem install cocoapods-art",
        },{
            before: "repo-art uses authentication as specified in your standard <a href=\"https://www.gnu.org/software/inetutils/manual/html_node/The-_002enetrc-file.html\" target=\"_blank\">netrc file</a>.",
            snippet: "machine $4\nlogin <USERNAME>\npassword <PASSWORD>"
        },{
            before: "To add an Artifactory Specs repo:",
            snippet: "pod repo-art add $1 \"$2/api/pods/$1\""
        }],
        read: [{
            before: "To resolve pods from an Artifactory specs repo that you added, you must add the following to your Podfile:",
            snippet: "plugin 'cocoapods-art', :sources => [\n  '$1'\n]"
        },{
            before: "Then you can use install as usual:",
            snippet: "pod install"
        }],
        deploy: [{
            before: "To deploy a pod into an Artifactory repository you need to use Artifactory's REST API or the Web UI.<br/>For example, to deploy a pod into this repository using the REST API, use the following command:",
            snippet: "curl <CURL_AUTH> -XPUT $2/$1/<TARGET_FILE_PATH> -T <PATH_TO_FILE>"
        }/*,  {
            before: "Artifactory can also function as a standalone Specs repo, which does not need to be backed by a Git repository.<br/>To push an index entry to the Specs repo on this repository use the following command:",
            snippet: "pod repo-art push $1 <NAME.podspec>",
            after: "Running the command without specifying a podspec will push all podspecs in the current working directory."
        }*/]
    },
    docker: {
        general: [{
            title_reverse_proxy: "Using Docker with Artifactory requires a reverse proxy such as Nginx or Apache. For more details please visit our <a href=\"http://www.jfrog.com/confluence/display/RTF/Docker+Repositories#DockerRepositories-RequirementforaReverseProxy(Nginx/Apache)\" target=\"_blank\">Docker Repositories</a> documentation.",
            title_insecure: "<br/>Not using an SSL certificate requires Docker clients to add an --insecure-registry flag to the <b>DOCKER_OPTS</b>",
            snippet_insecure: 'export DOCKER_OPTS+=" --insecure-registry <INSECURE_SNIP>"',
            after_example_server: "<br/>In this example we use <b>artprod.company.com</b> to represent the Docker repository in Artifactory.",
        },{
            before: "To login use the <i>docker login</i> command.",
            snippet: "docker login <DOCKER_SERVER>",
            after: "And provide your Artifactory username and password or API key.<br/>If anonymous access is enabled you do not need to login."
        },{
            before: "To manually set your credentials, copy the following snippet to your ~/.dockercfg file.",
            snippet: "{\n\t\"!https://<DOCKER_SERVER>\" : {\n\t\t\"auth\": \"<USERNAME>:<PASSWORD> (converted to base 64)\",\n\t\t\"email\": \"youremail@email.com\"\n\t}\n}",
        }],
        read: [{
            before: "To pull an image use the <i>docker pull</i> command specifying the docker image and tag.",
            snippet: "docker pull <DOCKER_SERVER>/<DOCKER_REPOSITORY>:<DOCKER_TAG>"
        }],
        deploy: [{
            before: "To push an image tag an image using the <i>docker tag</i> and then <i>docker push</i> command.",
            snippet: "docker tag ubuntu <DOCKER_SERVER>/<DOCKER_REPOSITORY>:<DOCKER_TAG>"
        }, {
            before: "",
            snippet: "docker push <DOCKER_SERVER>/<DOCKER_REPOSITORY>:<DOCKER_TAG>"
        }]
    },
    gitlfs: {
        read: [{
            before: "In order for your client to upload and download LFS blobs from artifactory, the [lfs] clause should be added to the <i>.lfsconfig</i> file of your Git repository in the following format:",
            snippet: "[lfs]\n" + "url = \"$2/api/lfs/$1\""
        }, {
            before: "You can also set LFS endpoints for different remotes on your repo (as supported by the Git LFS client). For example:",
            snippet: "[remote \"origin\"]\n" + "url = <URL>\n" + "fetch = +refs/heads/*:refs/remotes/origin/*\n" + "lfsurl = \"$2/api/lfs/$1\""
        }]
    },
    nuget: {
        general: [{
            before: "When using Artifactory as a NuGet repository you can either work with the NuGet client directly or with Visual Studio"
        },{
            before: "<b>NuGet Client Configuration</b><br/>To configure NuGet client to work with Artifactory you will need to add Artifactroy to the list of sources. To add this repository use the following command",
            snippet: "nuget sources Add -Name Artifactory -Source $2/api/nuget/$1"
        },{
            before: "To enable the use of NuGet API key, run the following command",
            snippet: "nuget setapikey <USERNAME>:<PASSWORD> -Source Artifactory"
        },{
            before: "<b>Visual Studio Configuration</b><br/>To configure the NuGet Visual Studio Extension to use Artifactory, you need to add Artifactory as another Package Source under NuGet Package Manager.<ol><li>Access the corresponding repositories in the \"Options\" window (Options | Tools) and select to add another Package Source.<br />Name: ENTER_RESOURCE_NAME (e.g. Artifactory NuGet repository)</li><li>Paste the snippet below in the URL field</li></ol>",
            snippet: "$2/api/nuget/$1",
            after: "<ol start=\"3\"><li>Make sure it is checked.</li></ol>"
        }],
        read:[{
            before: "<b>NuGet Client Resolve</b><br/>To resove using the NuGet client, run the following command",
            snippet: "nuget install <PACKAGE_NAME>"
        },{
            before: "To make sure your client will resolve from Artifactory verify it is the first in the list of sources, or run the following command",
            snippet: "nuget install <PACKAGE_NAME> -Source Artifactory"
        }],
        deploy: [{
            before: "Uploading packages to Artifactory can be done by running the following command:",
            snippet: "nuget push <PACKAGE_NAME> -Source Artifactory"
        },{
            before: "To support more manageable layouts and additional features such as cleanup, NuGet repositories support custom layouts. When pushing a package, you need to ensure that its layout matches the target repository’s layout:",
            snippet: "nuget push <PACKAGE> -Source $2/api/nuget/$1/<PATH_TO_FOLDER>"
        }]
    },
    ivy: {
        general: {
            title: "Click on \"Generate Ivy Settings\" in order to use Virtual or Remote repositories for resolution."
        }
    },
    maven: {
        general: {
            title: "Click on \"Generate Maven Settings\" in order to resolve artifacts through Virtual or Remote repositories."
        },
        deploy: {
            before: "To deploy build artifacts through Artifactory you need to add a deployment element with the URL of a target local repository to which you want to deploy your artifacts. For example:"
        }
    },
    npm: {
        general: [{
            title: "In order for your npm command line client to work with Artifactory you will firstly need to set the relevant authentication. For getting authentication details run the following command:",
            snippet: "curl -u<USERNAME>:<PASSWORD> $2/api/npm/auth"
        }, {
            before: "The response should be pasted in the <i>~/.npmrc</i> (in Windows %USERPROFILE%/<i>.npmrc</i>) file. Here is an example of the content of the file:",
            snippet: "_auth = <USERNAME>:<PASSWORD> (converted to base 64)\n" + "email = youremail@email.com\n" + "always-auth = true"
        }, {
            before: "Artifactory also support scoped packages. For getting authentication details run the following command:",
            snippet: "curl -u<USERNAME>:<PASSWORD> \"$2/api/npm/$1/auth/<SCOPE>\""
        }, {
            before: "The response should be pasted in the <i>~/.npmrc</i> (in Windows %USERPROFILE%/<i>.npmrc</i>) file. Here is an example of the content of the file:",
            snippet: "@<SCOPE>:registry=$2/api/npm/$1/\n" + "//$4/api/npm/$1/:_password=<BASE64_PASSWORD>\n" + "//$4/api/npm/$1/:username=<USERNAME>\n" + "//$4/api/npm/$1/:email=email@domain.com\n" + "//$4api/npm/$1/:always-auth=true\n"
        }, {
            before: "Run the following command to replace the default npm registry with an Artifactory repository:",
            snippet: "npm config set registry $2/api/npm/$1"
        }, {
            before: "For scoped package run the following command:",
            snippet: "npm config set @<SCOPE>:registry $2/api/npm/$1"
        }],
        read: [{
            before: "After adding Artifactory as the default repository you can install a package using the npm install command:",
            snippet: "npm install <PACKAGE_NAME>"
        }, {
            before: "To install a package by specifying Artifactory repository use the following npm command:",
            snippet: "npm install <PACKAGE_NAME> --registry $2/api/npm/$1"
        }],
        deploy: [{
            before: "To deploy your package to an Artifactory repository you can either add the following to the <i>package.json</i> file:",
            snippet: "\"publishConfig\":{\"registry\":\"$2/api/npm/$1\"}"
        }, {
            before: "And then you can simply run the default npm publish command:",
            snippet: "npm publish"
        }, {
            before: "Or provide the local repository to the npm publish command:",
            snippet: "npm publish --registry $2/api/npm/$1" }]
    },
    gems: {
        general: [{
            title: "For your gem client to upload and download Gems from this repository you need to add it to your <i>~/.gemrc</i> file using the following command:",
            snippet: "gem source -a http://<USERNAME>:<PASSWORD>@$4/artifactory/api/gems/$1/"
        }, {
            before: "If anonymous access is enabled you can also use the following:",
            snippet: "gem source -a $2/api/gems/$1/"
        }, {
            before: "To view a list of your effective sources and their order of resolution, run the following command:",
            snippet: "gem source --list",
            after: "Make sure that this repository is at the top of the list."
        }, {
            before: "If you want to setup the credentials for your gem tool either include your API_KEY in the <i>~/.gem/credentials</i> file, or run the following command:",
            snippet: "curl -u<USERNAME>:<PASSWORD> $2/api/gems/$1/api/v1/api_key.yaml > ~/.gem/credentials"
        }, {
            before: "<b>Running on Linux</b><br/>On Linux you may need to change the permissions of the credentials file to 600 by navigating to <i>~/.gem</i> directory and running:",
            snippet: "chmod 600 credentials"
        }, {
            before: "<b>Running on Windows</b><br/>On Windows, the credentials file is located at <i>%USERPROFILE%/.gem/credentials</i>. Note that you also need to set the API key encoding to be \"ASCII\".<br/> To generate the creadentials file run the following command from PowerShell:",
            snippet: "curl.exe -u<USERNAME>:<PASSWORD> $2/api/gems/$1/api/v1/api_key.yaml | Out-File ~/.gem/credentials -Encoding \"ASCII\""
        }, {
            before: "<b>API keys</b><br/>You can modify the credentials file manually and add different API keys. You can then use the following command to choose the relevant API key:",
            snippet: "gem push -k <KEY>"
        }],
        deploy: [{
            before: "In order to push gems to this repository, you can set the global variable $RUBYGEMS_HOST to point to it as follows:",
            snippet: "export RUBYGEMS_HOST=$2/api/gems/$1"
        }, {
            before: "You can also specify the target repository when pushing the gem by using the --host option:",
            snippet: "gem push <PACKAGE> --host $2/api/gems/$1"
        }],
        read: [{
            before: "After completing the configuration under General section above, simply execute the following command:",
            snippet: "gem install <PACKAGE>"
        }, {
            before: "The package will be resolved from the repository configured in your <i>~/.gemrc</i> file. You can also specify a source with the following command:",
            snippet: "gem install <PACKAGE> --source $2/api/gems/$1"
        }]
    },
    generic: {
        read: {
            before: "You can download a file directly using the following command:",
            snippet: "curl <CURL_AUTH> -O \"$2/$1/<TARGET_FILE_PATH>\""
        },
        deploy: {
            before: "You can upload any file using the following command:",
            snippet: "curl <CURL_AUTH> -T <PATH_TO_FILE> \"$2/$1/<TARGET_FILE_PATH>\""
        }
    },
    vagrant: {
        read: {
            before: "To provision a Vagrant box, all you need is to construct it's name in the following manner.",
            snippet: "vagrant box add \"$2/api/vagrant/$1/{boxName}\""
        },
        deploy: {
            before: "To deploy Vagrant boxes to this Artifactory repository using an explicit URL with Matrix Parameters use:",
            snippet: "curl <CURL_AUTH> -T <PATH_TO_FILE> \"$2/$1/{vagrantBoxName.box};box_name={name};box_provider={provider};box_version={version}\""
        }
    },
    vcs: {
        general: {
            title: "Artifactory supports downloading tags or branches using a simple GET request. You can also specify to download a specific tag or branch as a tar.gz or zip, and a specific file within a tag or branch as a zip file."
        },
        read: [{
            before: "Use the following command to list all tags:",
            snippet: "curl <CURL_AUTH> -XGET $2/api/vcs/tags/$1/<USER_ORG>/<REPO>"
        }, {
            before: "Use the following command to list all branches:",
            snippet: "curl <CURL_AUTH> -XGET $2/api/vcs/branches/$1/<USER_ORG>/<REPO>"
        }, {
            before: "Use the command below to download a tag. You can specify if the package will be downloaded as a tar.gz or a zip; default is tar.gz.",
            snippet: "curl <CURL_AUTH> -XGET $2/api/vcs/downloadTag/$1/<USER_ORG>/<REPO>/<TAG_NAME>?ext=<tar.gz/zip>"
        }, {
            before: "Use the following command to download a file within a tag as a zip:",
            snippet: "curl <CURL_AUTH> -XGET $2/api/vcs/downloadTag/$1/<USER_ORG>/<REPO>/<TAG_NAME>!<PATH_TO_FILE>?ext=zip"
        }, {
            before: "Use the command below to download a branch. You can specify a tar.gz or a zip by adding a parameter in the URL; default is tar.gz. (Downloading can be executed conditionally according to properties by specifying the properties query param. In this case only cached artifacts are searched.)",
            snippet: "curl <CURL_AUTH> -XGET $2/api/vcs/downloadBranch/$1/<USER_ORG>/<REPO>/<BRANCH_NAME>?ext=<tar.gz/zip>[&properties=key=value]"
        }, {
            before: "Use the following command to download a file within a branch as a zip:",
            snippet: "curl <CURL_AUTH> -XGET $2/api/vcs/downloadBranch/$1/<USER_ORG>/<REPO>/<BRANCH_NAME>!<PATH_TO_FILE>?ext=zip"
        }]
    },
    yum: {
        general: [{
            before: "To resolve <i>.rpm</i> files using the YUM client, edit or create the <i>artifactory.repo</i> file with root privileges:",
            snippet: "sudo vi /etc/yum.repos.d/artifactory.repo"
        }, {
            before: "Then edit the baseurl to point to the path of the <a href=\"https://www.jfrog.com/confluence/display/RTF/YUM+Repositories#YUMRepositories-YUMrepodataFolderDepth\" target=\"_blank\">repodata folder</a> according to configured repository depth.<br />If the configured depth is 0 the baseurl should point to the root of the repository.",
            snippet: "[Artifactory]\n" + "name=Artifactory\n" + "baseurl=http://<USERNAME>:<PASSWORD>@$4/artifactory/$1/<PATH_TO_REPODATA_FOLDER>\n" + "enabled=1\n" + "gpgcheck=0"
        }],
        read: [{

        }, {
            before: "To install a package execute:",
            snippet: "yum install <PACKAGE>"
        }]
    },
    sbt: {
        general: [{
            before: "You can define proxy repositories in the <i>~/.sbt/repositories</i> file in the following way:",
            snippet: "[repositories]\n" + "local\n" + "my-ivy-proxy-releases: $2/$1/, [organization]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]\n" + "my-maven-proxy-releases: $2/$1/"
        }, {
            before: "In order to specify that all resolvers added in the sbt project should be ignored in favor of those configured in the repositories configuration, add the following configuration option to the sbt launcher script:",
            snippet: "-Dsbt.override.build.repos=true",
            after: "You can add this setting to the <i>/usr/local/etc/sbtopts</i> file"
        }],
        read: {
            before: "Add the following to your <i>build.sbt</i> file:",
            snippet: "resolvers += \n" + "\"Artifactory\" at \"$2/$1/\""
        },
        deploy: [{
            before: "To publish <b>releases</b> add the following to your build.sbt:",
            snippet: "publishTo := Some(\"Artifactory Realm\" at \"$2/$1\")\n" + "credentials += Credentials(\"Artifactory Realm\", \"localhost\", \"<USERNAME>\", \"<PASSWORD>\")"
        }, {
            before: "To publish <b>snapshots</b> add the following to your build.sbt:",
            snippet: "publishTo := Some(\"Artifactory Realm\" at \"$2/$1;build.timestamp=\" + new java.util.Date().getTime)\n" + "credentials += Credentials(\"Artifactory Realm\", \"localhost\", \"<USERNAME>\", \"<PASSWORD>\")"
        }]
    },
    gradle: {
        general: {
            title: "Click on \"Generate Gradle Settings\" in order to use Virtual or Remote repositories for resolution."
        }
    }
};

export default snippets;