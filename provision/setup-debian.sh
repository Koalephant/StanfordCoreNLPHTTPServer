#!/bin/sh -eu

setup_debian() {

	if [ $(id -u) -ne 0 ]; then
		echo "Must be run as root, exiting"
		return 1
	fi

	setup_apt() {


	echo "Setting up Apt Preferences to disable Suggested package installation"

		cat <<-EOF > /etc/apt/apt.conf.d/99suggests
			APT::Install-Suggests "0";
			Apt::AutoRemove::SuggestsImportant "0";

EOF

		echo "Setting up Apt Preferences to disable Translated package installation"
		cat <<-EOF > /etc/apt/apt.conf.d/99translations
			Acquire::Languages "none";

EOF

		local release="$(lsb_release -c -s)"
		cat <<-EOF > /etc/apt/sources.list
			deb http://ftp.th.debian.org/debian/ ${release} main contrib non-free
			deb http://ftp.th.debian.org/debian/ ${release}-updates main contrib non-free
			deb http://ftp.th.debian.org/debian/ ${release}-backports main contrib non-free
			deb http://security.debian.org ${release}/updates main contrib non-free

EOF

		cat <<-EOT > /etc/apt/preferences.d/${release}-backports
			Package: openjdk-8-jdk openjdk-8-jre openjdk-8-jre-headless openjdk-8-jre-jamvm openjdk-8-jre-zero openjdk-8-source openjdk-8-dbg openjdk-8-demo
			Pin: release a=${release}-backports
			Pin-Priority: 500

EOT

		echo "Updating Apt"
		DEBIAN_FRONTEND=noninteractive aptitude update
		echo ""

		echo "Upgrading base packages"
		DEBIAN_FRONTEND=noninteractive aptitude full-upgrade -y
	}



	install_jdk() {
		echo "Installing Java 8"
		DEBIAN_FRONTEND=noninteractive aptitude -y install aptitude install openjdk-8-jre-headless openjdk-8-jdk ant
	}

	setup_apt "$@"
	install_jdk "$@"
}

setup_debian "$@"
