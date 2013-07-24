package com.comundus.opencms;

import junit.framework.TestCase;

public class TestHelperMethods extends TestCase {

		
	public void testIsInExcludesArray(){
		VfsSync out=new VfsSync();
		assertTrue(out.resourceIsInExcludesArray("a", new String[]{"gg","a","b"}));
	}
	
	public void testIsInExcludesArrayWithTrailingSlash(){
		VfsSync out=new VfsSync();
		assertTrue(out.resourceIsInExcludesArray("dir/", new String[]{"gg","a","b","dir"}));
	}
	
	public void testIsInExcludesArrayAsterisk(){
		VfsSync out=new VfsSync();
		assertTrue(out.resourceIsInExcludesArray("dir/", new String[]{"*"}));		
	}

	
	public void testIsInExcludesRealCase(){
		VfsSync out=new VfsSync();
		assertTrue(out.resourceIsInExcludesArray("/system/workplace/resources/tools/database/buttons/oamp/", 
				new String[]{"/system/workplace/workflow/", "/system/workplace/views/workflow/", "/system/workplace/resources/workflow/", 
			                    "/system/workplace/explorer/workflow/", "/system/workplace/resources/filetypes/alkacon-feed.png", 
								"/system/modules/com.alkacon.opencms.formgenerator/", "/system/workplace/resources/filetypes/oamp/", 
								"/system/workplace/admin/captcha/", "/system/workplace/resources/tools/database/icons/big/oamp/",
								"/system/workplace/resources/tools/database/buttons/oamp/", "/system/workplace/admin/database/formgenerator/, /system/workplace/admin/formgenerator/" }));
	}

	public void testIsInExcludesArrayPaths2(){
		VfsSync out=new VfsSync();
		assertTrue(out.resourceIsInExcludesArray("/system/workplace", 
				new String[]{"/system/workplace" }));
	}

}
