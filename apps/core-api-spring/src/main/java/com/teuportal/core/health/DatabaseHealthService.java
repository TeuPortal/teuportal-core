package com.teuportal.core.health;

import com.teuportal.core.company.CompanyUserRepository;
import com.teuportal.core.company.CompanyUserSummary;
import com.teuportal.core.storage.FileRepository;
import com.teuportal.core.storage.FolderRepository;
import com.teuportal.core.storage.FolderSummary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class DatabaseHealthService {

    private final CompanyUserRepository companyUserRepository;
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;

    public DatabaseHealthService(CompanyUserRepository companyUserRepository,
                                 FolderRepository folderRepository,
                                 FileRepository fileRepository) {
        this.companyUserRepository = companyUserRepository;
        this.folderRepository = folderRepository;
        this.fileRepository = fileRepository;
    }

    public Map<String, Object> diagnostics() {
        List<CompanyUserSummary> users = companyUserRepository.findRecentMembers(5);
        List<FolderSummary> folders = folderRepository.findRootFolders(5);
        long fileCount = fileRepository.countFiles();

        Map<String, Object> payload = new HashMap<>();
        payload.put("recentUsers", users);
        payload.put("rootFolders", folders);
        payload.put("fileCount", fileCount);
        return payload;
    }
}
