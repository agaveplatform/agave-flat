package org.iplantc.service.apps.util;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.managers.ApplicationManagerPermissionIT;
import org.iplantc.service.apps.model.JSONTestDataUtil;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: wcs
 * Date: 4/24/13
 * Time: 8:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class DomainSetup extends ApplicationManagerPermissionIT {

    private static final String SOFTWARE_OWNER =       "api_sample_user";       // default software owner if none given
    private static final String SYSTEM_OWNER =         "sysowner";      // default system owner
   
    private List<JSONObject> jsonStorageList;                           // list of base Storage objects as json
                                                                        // created from template directory
    private List<JSONObject> jsonExecutionList;                         // list of base Execution objects as json
                                                                        // created from template directory
    private List<JSONObject> jsonSoftwareList;
    private Map<String,Software> softwareMap = new HashMap<String, Software>();
    private Map<String,ExecutionSystem> executionSystemMap = new HashMap<String, ExecutionSystem>();
    private Map<String,StorageSystem> storageSystemMap = new HashMap<String, StorageSystem>();
    private SystemDao dao = new SystemDao();
    
    public SystemDao getDao() {
        return dao;
    }

    public Map<String, Software> getSoftwareMap() {
        return softwareMap;
    }

    public Map<String, ExecutionSystem> getExecutionSystemMap() {
        return executionSystemMap;
    }

    public Map<String, StorageSystem> getStorageSystemMap() {
        return storageSystemMap;
    }

    /*
     *                                                      *
     * Methods to handle json file retrieval                *
     *                                                      *
     */

    /**
     * Turn a file with json content into a JSONObject
     * @param pathToFile path to the file with json content
     * @return JSONObject from a file with json content
     */
    public JSONObject retrieveDataFile(String pathToFile){
        JSONObject json = null;
        try {
            String contents = FileUtils.readFileToString(new File(pathToFile));
            json = new JSONObject(contents);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * Turn a file with json content into a String
     * @param pathToFile path to the file with json content
     * @return String from a file with json content
     */
    public String retrieveDataFileAsString(String pathToFile){
        String contents = null;
        try {
            contents = FileUtils.readFileToString(new File(pathToFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contents;
    }

    /**
     * Write a file out to filesystem
     * @param file  File to write out
     * @param content  String content
     * @return  true for success
     */
    public boolean writeDataFile(File file, String content){
        try {
            FileUtils.writeStringToFile(file,content);
        } catch (IOException e) {
            String name = file.getName();
            System.out.println("failed writing "+name+" to file");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Write a file out to filesystem
     * @param pathToFile path to file
     * @param content    contents to write
     * @return  true if successful
     */
    public boolean writeDataFile(String pathToFile,String content){
        File file = FileUtils.getFile(pathToFile);
        return writeDataFile(file,content);
    }

    /*
     *                                                                      *
     * Methods to load up lists and maps of json objects and domain objects *
     *                                                                      *
     */

    /**
     * Given a list of files with json content create a list of JsonObjects
     * @param files
     * @return List of JSONObjects from a list of with File types
     * @throws java.io.IOException
     * @throws org.json.JSONException
     */
    private List<JSONObject> listOfJsonObjects(List<File> files) throws IOException, JSONException {
        List<JSONObject> jsonObjects = new ArrayList<JSONObject>();

        for(File file : files){
            jsonObjects.add(jtd.getTestDataObject(file));
        }
        return jsonObjects;
    }

    /**
     * Create a list of JSONObjects given a directory with json text files
     * @param directory root directory where files are kept
     * @return  a list of JSONObjects
     * @throws java.io.IOException
     * @throws org.json.JSONException
     */
    private List<JSONObject> collectFiles(String directory) throws IOException, JSONException {
        List<File> fileList;
        File dir = new File(directory);
        fileList = (List<File>) FileUtils.listFiles(dir, new String[]{"json"}, false);
        return listOfJsonObjects(fileList);
    }

    /**
     * turn all the domain object json lists into domain object maps for keyed access
     */
    public void fillSystemMaps(){

        for(JSONObject json : jsonStorageList){
            try {
                String key = (String)json.get("id");
//                System.out.println(key);
                storageSystemMap.put(key, StorageSystem.fromJSON(json));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        for(JSONObject json : jsonExecutionList){
            try {
                String key = (String)json.get("id");
//                System.out.println(key);
                executionSystemMap.put(key, ExecutionSystem.fromJSON(json));
            } catch (SystemArgumentException | JSONException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * default to load the files found in the storage, execution and software template directories
     * making lists of json objects and maps of domain objects. Maps of system domain objects an be used
     * to change values before persistence
     *
     */
    public void fillListsMaps(){
        jtd = JSONTestDataUtil.getInstance();
        try {
            jsonStorageList = collectFiles(STORAGE_SYSTEM_TEMPLATE_DIR);
            jsonExecutionList = collectFiles(EXECUTION_SYSTEM_TEMPLATE_DIR);
            jsonSoftwareList = collectFiles(SOFTWARE_SYSTEM_TEMPLATE_DIR);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        fillSystemMaps();
    }

    /**
     * This will fill up a list of Software domain objects that can be used for testing. They are not
     * persisted as yet but require that base systems data is in place in the database.
     *
     */
    public void fillSoftwareMap(){
        // this requires base system data in database to work
        for(JSONObject json : jsonSoftwareList){
            try {
                String key = (String)json.get("id");
                // must be able read remote system from db to create a software object.
                softwareMap.put(key, Software.fromJSON(json,SOFTWARE_OWNER));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     *                                                      *
     * Methods to create single domain objects from json    *
     *                                                      *
     */

    /**
     * Create a Software object from json object
     * @param json software json
     * @return Software object
     */
    public Software createSoftwareFromJson(JSONObject json){
        Software software = null;
        try {
            software = Software.fromJSON(json,SOFTWARE_OWNER);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return software;
    }

    /**
     * Create an execution system domain object from a json object
     * @param json JSONObject that represents and ExecutionSystem
     * @return ExecutionSystem that can be used in testing or persisted to database.
     */
    public ExecutionSystem createExecutionSystemFromJson(JSONObject json){
        ExecutionSystem executionSystem = null;
        try {
            executionSystem = ExecutionSystem.fromJSON(json);
        } catch (SystemArgumentException e) {
            e.printStackTrace();
        }
        return executionSystem;
    }

    /**
     * create a StorageSystem domain object from a json object
     * @param json
     * @return StrorageSystem
     */
    public StorageSystem createStorageSystemFromJson(JSONObject json){
        StorageSystem storageSystem = null;
        try{
            storageSystem = StorageSystem.fromJSON(json);
        } catch (SystemException e) {
            e.printStackTrace();
        }
        return storageSystem;
    }

    /**
     * Get a Software object from dehydrated json file on disk
     * @param pathToSoftwareFile path to file with dehydrated Software
     * @return Software object
     */
    public Software hydrateSoftware(String pathToSoftwareFile){
        JSONObject json = retrieveDataFile(pathToSoftwareFile);
        Software software = null;
        try {
             software = Software.fromJSON(json,SOFTWARE_OWNER);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return software;
    }

    /**
     * persist the template dir systems for base data set of domain objects for storage and
     * execution systems
     */
    public void persistDefaultTestSystemsData(){
        fillListsMaps();
        for(JSONObject storageJson : jsonStorageList){
            try {
                RemoteSystem system = (RemoteSystem)StorageSystem.fromJSON(storageJson);
                system.setOwner(SYSTEM_OWNER);         // default system owner
                dao.persist(system);
            } catch (SystemException e) {
                e.printStackTrace();
            }
        }

        for(JSONObject executionJson : jsonExecutionList){
            try {
                RemoteSystem system = (RemoteSystem)ExecutionSystem.fromJSON(executionJson);
                system.setOwner(SYSTEM_OWNER);         // default system owner
                dao.persist(system);
            } catch (SystemArgumentException e) {
                e.printStackTrace();
            }catch (ConstraintViolationException ce){
                //System.out.println("Continue with loading data if possible");
            }

        }
    }

    /**
     * Assuming Execution and Storage systems are in the database we can persist a set of Software domain objects
     * for testing
     */
    public void persistSoftwareDomain(){
        fillSoftwareMap(); // we assume persistSystemDomain has been executed before we begin due to Software.fromJson
                           // dependency on systems being available in the database.
        for(JSONObject softwareJson : jsonSoftwareList){
            try {
//                System.out.println(softwareJson.get("id"));
                Software software = Software.fromJSON(softwareJson,SOFTWARE_OWNER);
                software.setOwner(SOFTWARE_OWNER);
                SoftwareDao.persist(software);;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * persists a basic set of software that draws on known execution and storage systems
     */
    public void persistSoftwareAndSystemsDomain(){
        persistDefaultTestSystemsData();
        for(JSONObject softwareJson : jsonSoftwareList){
            try {
                Software software = Software.fromJSON(softwareJson,SOFTWARE_OWNER);
                software.setOwner(SYSTEM_OWNER);
                SoftwareDao.persist(software);;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * put a software json data file into the database
     * @param pathToSoftwareJsonFile  path to software json file
     */
    public void persistSingleSoftwareEntryFromFile(String pathToSoftwareJsonFile) {
        JSONObject softwareJson = retrieveDataFile(pathToSoftwareJsonFile);

        try {
            Software software = Software.fromJSON(softwareJson,SOFTWARE_OWNER);
            SoftwareDao.persist(software);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * persist a single Software domain object using a path to a json file into the database
     * @param pathToSoftwareJsonFile
     * @param owner
     */
    public void persistSingleSoftwareEntryFromFile(String pathToSoftwareJsonFile, String owner) {

        Software software = hydrateSoftware(pathToSoftwareJsonFile);
        software.setOwner(owner);
        SoftwareDao.persist(software);;
    }

    /**
     * persist a single Software domain object using a json string to the database
     * @param json
     */
    public void persistSingleSoftwareEntry(String json) {
        JSONObject softwareJson;
        try {
            softwareJson =new JSONObject(json);
            Software software = Software.fromJSON(softwareJson,SOFTWARE_OWNER);
            software.setOwner(SYSTEM_OWNER);
            SoftwareDao.persist(Software.fromJSON(softwareJson,SOFTWARE_OWNER));
        } catch (JSONException e) {
            System.out.println("failed to persist json \n"+json);
            e.printStackTrace();
        }
    }

    /**
     * persist a single Software domain object using a json object to the database
     * @param softwareJson
     */
    public void persistSingleSoftwareEntry(JSONObject softwareJson) {
        try {
            Software software = Software.fromJSON(softwareJson,SOFTWARE_OWNER);
            software.setOwner(SYSTEM_OWNER);
            SoftwareDao.persist(software);;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Setup a single base set of persisted domain objects of type storage and execution to test software registrations
     * @param storageFile
     * @param executeFile
     */
    public void getBaseSystemDomainSetForSoftwareRegistration(String storageFile, String executeFile){
        JSONObject storageJ = retrieveDataFile(STORAGE_SYSTEM_TEMPLATE_DIR+"/"+storageFile);
        JSONObject executeJ = retrieveDataFile(EXECUTION_SYSTEM_TEMPLATE_DIR+"/"+executeFile);
        RemoteSystem storageSystem = null;
        RemoteSystem executionSystem = null;
        try {
            storageSystem = (RemoteSystem)StorageSystem.fromJSON(storageJ);
            storageSystem.setOwner(SYSTEM_OWNER);         // default system owner
            executionSystem = (RemoteSystem)ExecutionSystem.fromJSON(executeJ);
            executionSystem.setOwner(SYSTEM_OWNER);
        } catch (SystemArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * gets the json data from a file as a String
     * @param pathToData
     * @return
     */
    public String getRegistrationData(String pathToData){
        return retrieveDataFileAsString(pathToData);
    }

    public void setupCondorTestDataStructure()
    {
        // the base data is already persisted for storage & execution systems
        // set the global default storage of iplant for wc-condor software.
        SystemDao idao = new SystemDao();
        RemoteSystem iplantStorage  = idao.findBySystemId("data.iplantcollaborative.org");
        iplantStorage.setGlobalDefault(true);
        iplantStorage.setPubliclyAvailable(true);
        idao.persist(iplantStorage);
//        System.out.println();
    }

    /**
     *
     * These are simple methods for setting up base data for a particular test scenario
     *
     *
     */

    public void baseDataForSoftwareRegistrationTest(){
        //GSqlData g = new GSqlData("DomainSetup");
        //System.out.println("Total records in all tables "+g.totalTableRecords());
        //g.lockAndWipeTables();
       // System.out.println("Total records in all tables " + g.totalTableRecords());
//        System.out.println("\n\n");
        persistDefaultTestSystemsData();
        //System.out.println("Total records in all tables "+g.totalTableRecords());
//        System.out.println("\n\n");
        //g.closeConnection("DomainSetup");
    }

    /**
     * Deletes all Software objects from the db.
     */
    public void clearSoftware() throws Exception
    {
        Session session = null;
        try
        {
            HibernateUtil.beginTransaction();
            session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.disableAllFilters();

            session.createQuery("DELETE SoftwareParameter").executeUpdate();
            session.createQuery("DELETE SoftwareInput").executeUpdate();
            session.createQuery("DELETE SoftwareOutput").executeUpdate();
            session.createSQLQuery("delete from softwares_parameters").executeUpdate();
            session.createSQLQuery("delete from softwares_inputs").executeUpdate();
            session.createSQLQuery("delete from softwares_outputs").executeUpdate();
            session.createQuery("DELETE SoftwarePermission").executeUpdate();
            session.createQuery("DELETE SoftwareParameterEnumeratedValue").executeUpdate();
            session.createQuery("DELETE SoftwareEvent").executeUpdate();
            session.createQuery("DELETE Software").executeUpdate();
            session.flush();
        }
        finally
        {
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
        }
    }

    /**
     * Deletes all execution and storage systems from the db.
     */
    public void clearSystems()
    {
        Session session = null;
        try
        {
            HibernateUtil.beginTransaction();
            session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.disableAllFilters();

            session.createQuery("DELETE ExecutionSystem").executeUpdate();
            session.createQuery("DELETE BatchQueue").executeUpdate();
            session.createQuery("DELETE AuthConfig").executeUpdate();
            session.createQuery("DELETE LoginConfig").executeUpdate();
            session.createQuery("DELETE CredentialServer").executeUpdate();
            session.createQuery("DELETE TransferTask").executeUpdate();
            session.createQuery("DELETE RemoteConfig").executeUpdate();
            session.createQuery("DELETE StorageSystem").executeUpdate();
            session.createQuery("DELETE StorageConfig").executeUpdate();
            session.createQuery("DELETE SystemRole").executeUpdate();
            session.createQuery("DELETE SystemPermission").executeUpdate();
            session.createSQLQuery("delete from userdefaultsystems").executeUpdate();
            session.flush();
        }
        finally
        {
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
        }
    }
}

