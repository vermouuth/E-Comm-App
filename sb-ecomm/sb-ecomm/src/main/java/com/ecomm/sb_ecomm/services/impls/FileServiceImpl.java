package com.ecomm.sb_ecomm.services.impls;

import com.ecomm.sb_ecomm.services.FileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {
    @Override
    public String uploadImage(String path, MultipartFile image) throws IOException {

        String fileName = image.getOriginalFilename();

        String uniqueId =  UUID.randomUUID().toString();
        String newFileName =  uniqueId.concat(fileName.substring(fileName.lastIndexOf('.')));
        String filePath = path + File.separator + newFileName;

        File folder = new File(path);
        if(!folder.exists()){  folder.mkdirs();}

        Files.copy(image.getInputStream(),Paths.get(filePath));

        return newFileName;
    }
}
