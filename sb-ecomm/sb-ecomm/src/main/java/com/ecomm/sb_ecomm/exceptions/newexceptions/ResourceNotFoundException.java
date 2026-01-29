package com.ecomm.sb_ecomm.exceptions.newexceptions;

public class ResourceNotFoundException extends RuntimeException {

    String resourceName;
    String filed;
    String fieldName;
    Long filedId;


    public ResourceNotFoundException(String resourceName , String filed ,String filedName)
    {
        super(String.format("%s not found with %s: %s" , resourceName, filed , filedName));
        this.resourceName = resourceName;
        this.filed = filed;
        this.fieldName = fieldName;
    }


    public ResourceNotFoundException(String resourceName , String filed ,Long filedId)
    {
        super(String.format("%s not found with %s: %s" , resourceName, filed , filedId));
        this.resourceName = resourceName;
        this.filed = filed;
        this.filedId = filedId;

    }

    public ResourceNotFoundException(){}
}
