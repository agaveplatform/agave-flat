package org.iplantc.service.metadata.model.enumerations;

import com.google.common.base.Enums;

public enum PermissionType
{

	NONE, READ, WRITE, EXECUTE, READ_WRITE, READ_EXECUTE, WRITE_EXECUTE, ALL, READ_PERMISSION, WRITE_PERMISSION, READ_WRITE_PERMISSION, UNKNOWN;

	public boolean canRead() {
		return (this.equals(ALL) ||
				this.equals(READ) || 
				this.equals(READ_PERMISSION) || 
				this.equals(READ_WRITE) ||
				this.equals(READ_WRITE_PERMISSION) ||
				this.equals(READ_EXECUTE));
	}
	
	public boolean canWrite() {
		return (this.equals(ALL) ||
				this.equals(WRITE) || 
				this.equals(WRITE_PERMISSION) || 
				this.equals(READ_WRITE) ||
				this.equals(READ_WRITE_PERMISSION) || 
				this.equals(WRITE_EXECUTE));
	}
	
	public boolean canExecute() {
		return (this.equals(ALL) ||
				this.equals(EXECUTE) || 
				this.equals(READ_EXECUTE) ||
				this.equals(WRITE_EXECUTE));
	}

	public PermissionType add(PermissionType newPermission)
	{
		if (newPermission.equals(this)) 
		{
			return newPermission;
		} 
		else if (newPermission.equals(ALL) || this.equals(ALL)) 
		{
			return ALL;
		} 
		else if (newPermission.canRead() && newPermission.canWrite())
		{
			if (canExecute()) 
				return ALL;
			else 
				return READ_WRITE;
		} 
		else if (newPermission.canRead() && newPermission.canExecute())
		{	
			if (canWrite()) 
				return ALL;
			else 
				return READ_EXECUTE;
		}	
		else if (newPermission.canWrite() && newPermission.canExecute())
		{	
			if (canRead()) 
				return ALL;
			else 
				return READ_WRITE;
		}
		else if (newPermission.canRead())
		{	
			if (canWrite() && canExecute()) 
				return ALL;
			else if (canWrite())
				return READ_WRITE;
			else if (canExecute())
				return READ_EXECUTE;
			else 
				return READ;
		}
		else if (newPermission.canWrite())
		{	
			if (canRead() && canExecute()) 
				return ALL;
			else if (canRead())
				return READ_WRITE;
			else if (canExecute())
				return WRITE_EXECUTE;
			else 
				return WRITE;
		}
		else if (newPermission.canExecute())
		{	
			if (canRead() && canWrite()) 
				return ALL;
			else if (canRead())
				return READ_EXECUTE;
			else if (canWrite())
				return WRITE_EXECUTE;
			else
				return EXECUTE;
		}
		else {
			return this;
		}
	}

	public int getUnixValue() 
	{
		if (this.equals(EXECUTE)) {
			return 1;
		} else if (this.equals(WRITE) || this.equals(WRITE_PERMISSION)) {
			return 2;
		} else if (this.equals(WRITE_EXECUTE)) {
			return 3;
		} else if (this.equals(READ) || this.equals(READ_PERMISSION)) {
			return 4;
		} else if (this.equals(READ_EXECUTE)) {
			return 5;
		} else if (this.equals(READ_WRITE) || this.equals(READ_WRITE_PERMISSION)) {
			return 6;
		} else if (this.equals(ALL)) {
			return 7;
		} else {
			return 0;
		}
	}

	public static String supportedValuesAsString()
	{
		return ALL + ", " + READ + ", " + WRITE + ", " + READ_WRITE + ", " + EXECUTE + ", " + READ_EXECUTE + ", " + WRITE_EXECUTE + "," + NONE;
	}
	
	@Override
	public String toString() {
		return name();
	}

	public static PermissionType getIfPresent(String name){
		return Enums.getIfPresent(PermissionType.class, name).or(PermissionType.UNKNOWN);
	}
		 
}
