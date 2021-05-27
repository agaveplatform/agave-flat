/**
 * 
 */
package org.iplantc.service.jobs.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.apps.Settings;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.util.ServiceUtils;
import org.iplantc.service.transfer.model.TransferTask;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Entity class for persisting job events. This creates a history log 
 * per job that can be queried and/or mined 
 * 
 * @author dooley
 *
 */
@Entity
@Table(name = "jobevents")
@FilterDef(name="jobEventTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="jobEventTenantFilter", condition="tenant_id=:tenantId"))
public class JobEvent {

	private Long id;
	private String status;
	private String description;
	private String ipAddress;
	private String createdBy;
	private Date created;
	private String tenantId;
	private String uuid;
		
	private Job job;
	private TransferTask transferTask;
	
	
	private JobEvent() {
		this.tenantId = TenancyHelper.getCurrentTenantId();
		this.uuid = new AgaveUUID(UUIDType.JOB_EVENT).toString();
        this.created = new DateTime().toDate();
	}

	public JobEvent(String status, String description, String createdBy) {
		this();
		this.status = status;
		this.description = description;
		this.ipAddress = ServiceUtils.getLocalIP();
		this.createdBy = createdBy;
	}
	
	 // Constructor.  The job and jobStatus parms cannot be null.
    public JobEvent(String uuid, Long id, Job job, JobStatusType jobStatus, String description, Date created, String createdBy, String tenantId, String ipAddress, TransferTask transferTask)
    {
        this.uuid = uuid;
        this.id = id;
        this.job = job;
        this.status = jobStatus.name();
        this.description = description;
        this.createdBy = createdBy;
        this.tenantId = tenantId;
        this.created = created;
        this.ipAddress = ipAddress;
        if (transferTask != null) {
            this.transferTask = transferTask;
        }
    }
   
	public JobEvent(JobStatusType status, String description, String createdBy)
	{
		this(status.name(), description, createdBy);
	}
	
	public JobEvent(Job job, JobStatusType status, String description, String createdBy)
	{
		this(status.name(), description, createdBy);
		setJob(job);
	}
	
	public JobEvent(JobStatusType status, String description, TransferTask transferTask, String createdBy)
	{
		this(status.name(), description, createdBy);
		setTransferTask(transferTask);
	}

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue
	@Column(name = "id", unique = true, nullable = false)
	public Long getId()
	{
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id)
	{
		this.id = id;
	}

	/**
	 * @return the job
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "job_id", nullable = false)
	public Job getJob()
	{
		return job;
	}

	/**
	 * @param job the job to set
	 */
	public void setJob(Job job)
	{
		this.job = job;
	}

	/**
	 * @return the status
	 */
	@Column(name = "status", nullable = false, length = 32)
	public String getStatus()
	{
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status)
	{
		this.status = status;
	}

	/**
	 * @return the username
	 */
	@Column(name = "created_by", nullable = false, length = 128)
	public String getCreatedBy()
	{
		return createdBy;
	}

	/**
	 * @param username the creator to set
	 */
	public void setCreatedBy(String createdBy)
	{
		this.createdBy = createdBy;
	}

	/**
	 * @return the message
	 */
	@Column(name = "description")
	public String getDescription()
	{
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description)
	{
		this.description = description;
	}

	/**
	 * @return the ipAddress
	 */
	@Column(name = "ip_address", nullable = false, length = 15)
	public String getIpAddress()
	{
		return ipAddress;
	}

	/**
	 * @param ipAddress the ipAddress to set
	 */
	public void setIpAddress(String ipAddress)
	{
		this.ipAddress = ipAddress;
	}

	/**
	 * @return the tenantId
	 */
	@Column(name = "tenant_id", nullable=false, length = 128)
	public String getTenantId()
	{
		return tenantId;
	}

	/**
	 * @param tenantId the tenantId to set
	 */
	public void setTenantId(String tenantId)
	{
		this.tenantId = tenantId;
	}

	/**
	 * @return the transferTask
	 */
	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "transfertask")
	public TransferTask getTransferTask()
	{
		return transferTask;
	}
	

	/**
	 * @param transferTask the transferTask to set
	 */
	public void setTransferTask(TransferTask transferTask)
	{
		
		this.transferTask = transferTask;
		
	}

	/**
     * @return the uuid
     */
    @Column(name = "uuid")
    public String getUuid()
    {
        return uuid;
    }

    /**
     * @param uuid the uuid to set
     */
    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }
    
    /**
	 * @return the created
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false)
	public Date getCreated()
	{
		return created;
	}

	/**
	 * @param created the created to set
	 */
	public void setCreated(Date created)
	{
		this.created = created;
	}
	
	public String toString() {
		return job + " " + status + " " + new DateTime(created).toString();
	}
	
	public String toJSON(Software software) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode()
                .put("status", getStatus())
                .put("createdBy", getCreatedBy())
                .put("description", getDescription())
                .put("created", new DateTime(getCreated()).toString())
                .put("id", getUuid());
        ObjectNode links = json.putObject("_links");
        links.put("self", mapper.createObjectNode()
                .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + job.getUuid() + "/history/" + getUuid()));
        links.put("job", mapper.createObjectNode()
                .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + job.getUuid()));
        links.put("profile", mapper.createObjectNode()
                .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + getCreatedBy()));
        
        return json.toString();
    }
}
