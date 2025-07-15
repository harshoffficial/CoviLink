package com.harsh.covilink;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryConfig {
    
    private static Cloudinary cloudinary;
    
    public static Cloudinary getCloudinary() {
        if (cloudinary == null) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dui9ioqwm");
            config.put("api_key", "969396162765785");
            config.put("api_secret", "7a0rWVE158DZArTU0vxrAcC0zng");
            
            cloudinary = new Cloudinary(config);
        }
        return cloudinary;
    }
    
    public static Map<String, Object> getUploadOptions() {
        return ObjectUtils.asMap(
            "folder", "covilink_profiles",
            "public_id", "profile_" + System.currentTimeMillis(),
            "overwrite", true,
            "resource_type", "image"
        );
    }
} 