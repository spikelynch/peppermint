# Must be run as the user used to run peppermint

# Install dependencies
#
# Check for groovy Install
#
GROOVY_VERSION="2.5.2"
CURRENT_USER=`whoami`
if ! type "groovy" > /dev/null; then
  echo "Installing Groovy ${GROOVY_VERSION} as ${CURRENT_USER}..."
  # Guess the OS: Supports yum and apt-get only
  if [ -f "/etc/redhat-release" ]; then
    yum install zip curl -y
  else
    apt-get update
    apt-get install zip unzip curl -y --allow-unauthenticated
  fi
  curl -s "https://get.sdkman.io" | bash
  bash -c "source ${HOME}/.sdkman/bin/sdkman-init.sh && sdk install groovy ${GROOVY_VERSION} < /dev/null"
fi
export JAVA_OPTS='-Divy.message.logger.level=4 -Dgroovy.grape.report.downloads=true'
bash -c "source ${HOME}/.sdkman/bin/sdkman-init.sh && groovy support/get-script-deps.groovy"
