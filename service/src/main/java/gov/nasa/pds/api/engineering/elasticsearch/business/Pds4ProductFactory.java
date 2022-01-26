package gov.nasa.pds.api.engineering.elasticsearch.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.pds.model.Pds4Metadata;
import gov.nasa.pds.model.Pds4MetadataDataFiles;
import gov.nasa.pds.model.Pds4MetadataLabelFile;
import gov.nasa.pds.model.Pds4Product;


/**
 * Creates Pds4Product object from Elasticsearch key-value field map.
 * @author karpenko
 */
public class Pds4ProductFactory
{
	private static final Logger log = LoggerFactory.getLogger(Pds4ProductFactory.class);

    // JSON BLOB
    public static final String FLD_JSON_BLOB = BlobUtil.JSON_BLOB_PROPERTY;
    public static final String FLD_XML_BLOB = BlobUtil.XML_BLOB_PROPERTY;
    
    // Data File Info
    public static final String FLD_DATA_FILE_NAME = "ops:Data_File_Info/ops:file_name";
    public static final String FLD_DATA_FILE_CREATION = "ops:Data_File_Info/ops:creation_date_time";
    public static final String FLD_DATA_FILE_REF = "ops:Data_File_Info/ops:file_ref";
    public static final String FLD_DATA_FILE_SIZE = "ops:Data_File_Info/ops:file_size";
    public static final String FLD_DATA_FILE_MD5 = "ops:Data_File_Info/ops:md5_checksum";
    public static final String FLD_DATA_FILE_MIME_TYPE = "ops:Data_File_Info/ops:mime_type";

    // Label Info
    public static final String FLD_LABEL_FILE_NAME = "ops:Label_File_Info/ops:file_name";
    public static final String FLD_LABEL_FILE_CREATION = "ops:Label_File_Info/ops:creation_date_time";
    public static final String FLD_LABEL_FILE_REF = "ops:Label_File_Info/ops:file_ref";
    public static final String FLD_LABEL_FILE_SIZE = "ops:Label_File_Info/ops:file_size";
    public static final String FLD_LABEL_FILE_MD5 = "ops:Label_File_Info/ops:md5_checksum";

    // Node Name
    public static final String FLD_NODE_NAME = "ops:Harvest_Info/ops:node_name";
    
    
    /**
     * Create Pds4Product object from Elasticsearch key-value field map.
     * @param lidvid product LIDVID
     * @param fieldMap key-value field map
     * @return new Pds4Product object
     */
    public static Pds4Product createProduct(String lidvid, Map<String, Object> fieldMap, boolean isJSON)
    {
        Pds4Product prod = new Pds4Product();
        prod.setId(lidvid);
        
        if(fieldMap == null) return prod;
                
        // Pds4 JSON BLOB
        String decoded_blob = null;
        try
        { 
        	if (isJSON) decoded_blob = BlobUtil.blobToString(String.valueOf(fieldMap.get(FLD_JSON_BLOB)));
        	else
        	{
        		int first,last;
        		decoded_blob = BlobUtil.blobToString(String.valueOf(fieldMap.get(FLD_XML_BLOB)));
        		decoded_blob = decoded_blob.replaceAll("\r", "");
        		first = decoded_blob.indexOf("<?");
        		while (0 <= first)
        		{
        			last = decoded_blob.indexOf("?>", first+2);
        			decoded_blob = decoded_blob.replace (decoded_blob.substring(first, last+2), "");
        			first = decoded_blob.indexOf("<?");
        		}
        		decoded_blob = decoded_blob.strip();
        	}
        }
        catch (Exception e)
        {
        	log.error("Could not convert the given blob", e);
        	decoded_blob = "Could not decode blob. See logs for error details.";
        }
        prod.setPds4(decoded_blob);
        // Metadata
        prod.setMetadata(createMetadata(fieldMap));

        return prod;
    }

    
    private static Pds4Metadata createMetadata(Map<String, Object> fieldMap)
    {
        Pds4Metadata meta = new Pds4Metadata();

        meta.setNodeName((String)fieldMap.get(FLD_NODE_NAME));
        meta.setLabelFile(createLabelFile(fieldMap));
        meta.setDataFiles(createDataFiles(fieldMap));
        
        return meta;
    }
    
    
    private static Pds4MetadataLabelFile createLabelFile(Map<String, Object> fieldMap)
    {
        Object obj = fieldMap.get(FLD_LABEL_FILE_NAME);
        if(obj == null) return null;

        Pds4MetadataLabelFile item = new Pds4MetadataLabelFile();

        String val = (String)obj;
        item.setFileName(val);
        
        val = (String)fieldMap.get(FLD_LABEL_FILE_CREATION);
        item.setCreationDate(val);
        
        val = (String)fieldMap.get(FLD_LABEL_FILE_REF);
        item.setFileRef(val);
        
        val = (String)fieldMap.get(FLD_LABEL_FILE_SIZE);
        item.setFileSize(val);

        val = (String)fieldMap.get(FLD_LABEL_FILE_MD5);
        item.setMd5Checksum(val);

        return item;
    }
    

    @SuppressWarnings("rawtypes")
    private static List<Pds4MetadataDataFiles> createDataFiles(Map<String, Object> fieldMap)
    {
        Object obj = fieldMap.get(FLD_DATA_FILE_NAME);
        if(obj == null) return null;

        if(obj instanceof List)
        {
            List nameList = (List)obj;
            List dateList = (List)fieldMap.get(FLD_DATA_FILE_CREATION);
            List refList = (List)fieldMap.get(FLD_DATA_FILE_REF);
            List sizeList = (List)fieldMap.get(FLD_DATA_FILE_SIZE);
            List md5List = (List)fieldMap.get(FLD_DATA_FILE_MD5);
            List mimeList = (List)fieldMap.get(FLD_DATA_FILE_MIME_TYPE);            
            
            List<Pds4MetadataDataFiles> items = new ArrayList<>(nameList.size());
            
            for(int i = 0; i < nameList.size(); i++)
            {
                Pds4MetadataDataFiles item = new Pds4MetadataDataFiles();
                items.add(item);
                item.setFileName((String)nameList.get(i));
                item.setCreationDate((String)dateList.get(i));
                item.setFileRef((String)refList.get(i));
                item.setFileSize((String)sizeList.get(i));
                item.setMd5Checksum((String)md5List.get(i));
                item.setMimeType((String)mimeList.get(i));
            }
            
            return items;
        }
        else
        {
            String val = (String)obj;
            List<Pds4MetadataDataFiles> items = new ArrayList<>(1);
            
            Pds4MetadataDataFiles item = new Pds4MetadataDataFiles();
            items.add(item);
            item.setFileName(val);
            
            val = (String)fieldMap.get(FLD_DATA_FILE_CREATION);
            item.setCreationDate(val);
            
            val = (String)fieldMap.get(FLD_DATA_FILE_REF);
            item.setFileRef(val);
            
            val = (String)fieldMap.get(FLD_DATA_FILE_SIZE);
            item.setFileSize(val);

            val = (String)fieldMap.get(FLD_DATA_FILE_MD5);
            item.setMd5Checksum(val);

            val = (String)fieldMap.get(FLD_DATA_FILE_MIME_TYPE);
            item.setMimeType(val);
            
            return items;
        }
    }
}
