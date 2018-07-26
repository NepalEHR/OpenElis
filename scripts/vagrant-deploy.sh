PATH_OF_CURRENT_DIRECTORY="$(pwd)"

#source $PATH_OF_CURRENT_DIRECTORY/vagrant/vagrant_functions.sh

cd ..

OPENELIS_HOME="$(pwd)"

echo "$OPENELIS_HOME"
ant dist

cd "$OPENELIS_HOME/openelis/dist/"
echo "$OPENELIS_HOME/openelis/dist/"
unzip -q openelis.war -d "openelis"

cd "$BAHMNI_HOME/"

sudo service bahmni-lab stop
sudo rm -rf /opt/bahmni-lab/bahmni-lab/*
sudo cp -r $OPENELIS_HOME/openelis/dist/openelis/* /opt/bahmni-lab/bahmni-lab/.
sudo chown -R bahmni:bahmni /opt/bahmni-lab/bahmni-lab/*
sudo service bahmni-lab start

cd "$PATH_OF_CURRENT_DIRECTORY"