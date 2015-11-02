Vagrant.configure(2) do |config|
	config.vm.box = "koalephant/debian81-dev"

	config.vm.synced_folder ".", "/vagrant/stanford-corenlp-http-server"

	config.vm.provider "parallels" do |vm|
		vm.memory = 3100
		vm.name = "StanfordCoreNLPHTTPServer"
	end

	config.vm.provider "virtualbox" do |vm|
		vm.memory = 3100
	end

	config.vm.define "StanfordCoreNLPHTTPServer"

end
