package com.ecomm.sb_ecomm.exceptions.newexceptions;

public class    ResourceNotFoundException extends RuntimeException {

    String resourceName;
    String field;
    String fieldName;
    Long fieldId;


    public ResourceNotFoundException(String resourceName , String field, String filedName)
    {
        super(String.format("%s not found with %s: %s" , resourceName, field, filedName));
        this.resourceName = resourceName;
        this.field = field;
        this.fieldName = filedName;
    }


    public ResourceNotFoundException(String resourceName , String field, Long fieldId)
    {
        super(String.format("%s not found with %s: %s" , resourceName, field, fieldId));
        this.resourceName = resourceName;
        this.field = field;
        this.fieldId = fieldId;

    }

    public ResourceNotFoundException(){}
}
