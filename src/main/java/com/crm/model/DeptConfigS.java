package com.crm.model;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "departments")
public class DeptConfigS {
	@Id
	private String id;

	//	@Field(name = "dept_name")
	private String deptName;

	//	@Field(name = "dept_coord_role")
	private String deptCoordRole;

	//	@Field(name = "dept_admin_role")
	private List<String> deptAdminRole;

	//	@Field(name = "dept_display_name")
	private String deptDisplayName;

	//	@Field(name = "dept_role_display_name")
	private String deptRoleDisplayName;

	//	@Field(name = "branch")
	private String branch;

	@Field(name = "section")
	private List<String> section;

	//	@Field(name = "roles")
	private List<Roles> roles;

	//	@Field(name = "sub_section")
	private List<String> subSection;

	//	@Field(name = "block_no")
	private List<String> blockNo;

	//	@Field(name = "cau")
	private String cau;

	//	@Field(name = "Pkl_Directorate")
	private String pklDirectorate;
	@Builder.Default
	private boolean isSuperAdmin = false;


//	for new structure

	private int fromBlock;//for block number
	private int toBlock;//for block number
	private String sec;//for Section
	private HashMap<String, String> subSec;
	private HashMap<Integer, String> sectionBlock;//for list of range [fromBlock - toBlock] using loop
	@Builder.Default
	private boolean isIndependent = false;
	private String dependsOn;
	@Builder.Default
	@JsonProperty("isActive")
	private boolean isActive = false;
	private String reportsTo;




	public int addToRoles( Roles role)
	{
		try
		{
			if(this.roles == null)
			{
				this.roles = new ArrayList<Roles>();
			}

			this.roles.add(role);
			return 1;
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			return -1;
		}

	}




	public int addToDeptAdminRole(String roleName) {
		try
		{
			if(this.deptAdminRole == null)
			{
				this.deptAdminRole = new ArrayList<String>();
			}

			this.deptAdminRole.add(roleName);
			return 1;
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			return -1;
		}
	}




}