**You can find ARCLib prototypes here: https://github.com/LIBCAS/ARCLib-Prototypes** 

**Database**
* user: **arclib**
* password: **vuji61oilo**
* url: **jdbc:postgresql://localhost:5432/arclib**


Aditional configuration:

**ClamAV**

* your local antivirus should be deactivated otherwise it blocks **eicar** test file
* ClamAV antivirus has to be installed, after instalation:
  * create **database** and **quarantine** folders inside ClamAV folder
  * copy **freshclam.conf** from prototype 3 resources folder to ClamAV folder
  * run **freshclam.exe**
* **clamscan** command has to be added to PATH variable
* there must be **CLAMAV** environment variable pointing to CLAMAV directory

**Droid**

* path to the directory with the binary of DROID must be added to PATH

**Solr**

* Solr 7.0.1 has to be installed
* **solr** command has to be added to PATH variable
* port **8983** should be available for Solr
* after Solr instalation execute:
```
solr start
solr create -c arclib_xml
solr stop -all
```
* copy **standalone.conf** and **schema.xml** from **config** folder to *solrhome*/server/solr/arclib_xml/conf
