package com.tw.go.plugin.nuget.apimpl;

import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfigurations;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRepositoryPoller;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.validation.Errors;
import com.thoughtworks.go.plugin.api.validation.ValidationError;
import com.tw.go.plugin.nuget.NuGet;
import com.tw.go.plugin.nuget.NuGetParams;
import com.tw.go.plugin.nuget.config.NuGetPackageConfig;
import com.tw.go.plugin.nuget.config.NuGetRepoConfig;

public class NuGetPoller implements PackageRepositoryPoller {
    private static Logger LOGGER = Logger.getLoggerFor(NuGetPoller.class);

    public PackageRevision getLatestRevision(PackageConfigurations packageConfig, PackageConfigurations repoConfig) {
        LOGGER.info(String.format("getLatestRevision called with packageId %s, for repo: %s",
                packageConfig.get(NuGetPackageConfig.PACKAGE_ID).getValue(), repoConfig.get(NuGetRepoConfig.REPO_URL).getValue()));
        validateConfig(repoConfig, packageConfig);
        NuGetPackageConfig nuGetPackageConfig = new NuGetPackageConfig(packageConfig);
        NuGetParams params = new NuGetParams(
                new NuGetRepoConfig(repoConfig).getRepoUrl(),
                nuGetPackageConfig.getPackageId(),
                nuGetPackageConfig.getPollVersionFrom(),
                nuGetPackageConfig.getPollVersionTo(), null);
        PackageRevision packageRevision = executeNuGetCmd(params);
        LOGGER.info(String.format("getLatestRevision returning with %s, %s",
                packageRevision.getRevision(), packageRevision.getTimestamp()));
        return packageRevision;
    }

    public PackageRevision latestModificationSince(PackageConfigurations packageConfig, PackageConfigurations repoConfig, PackageRevision previouslyKnownRevision) {
        LOGGER.info(String.format("latestModificationSince called with packageId %s, for repo: %s",
                packageConfig.get(NuGetPackageConfig.PACKAGE_ID).getValue(), repoConfig.get(NuGetRepoConfig.REPO_URL).getValue()));
        validateConfig(repoConfig, packageConfig);
        NuGetPackageConfig nuGetPackageConfig = new NuGetPackageConfig(packageConfig);
        NuGetParams params = new NuGetParams(
                new NuGetRepoConfig(repoConfig).getRepoUrl(),
                nuGetPackageConfig.getPackageId(),
                nuGetPackageConfig.getPollVersionFrom(),
                nuGetPackageConfig.getPollVersionTo(),
                previouslyKnownRevision);
        PackageRevision updatedPackage = executeNuGetCmd(params);
        if (updatedPackage == null) {
            LOGGER.info(String.format("no modification since %s", previouslyKnownRevision.getRevision()));
            return null;
        }
        LOGGER.info(String.format("latestModificationSince returning with %s, %s",
                updatedPackage.getRevision(), updatedPackage.getTimestamp()));
        if (updatedPackage.getTimestamp().getTime() < previouslyKnownRevision.getTimestamp().getTime())
            LOGGER.warn(String.format("Updated Package %s published earlier (%s) than previous (%s, %s)",
                    updatedPackage.getRevision(), updatedPackage.getTimestamp(), previouslyKnownRevision.getRevision(), previouslyKnownRevision.getTimestamp()));
        return updatedPackage;
    }

    private void validateConfig(PackageConfigurations repoConfig, PackageConfigurations packageConfig) {
        Errors errors = new Errors();
        new PluginConfig().isRepositoryConfigurationValid(repoConfig, errors);
        new PluginConfig().isPackageConfigurationValid(packageConfig, repoConfig, errors);
        if (errors.hasErrors()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (ValidationError validationError : errors.getErrors()) {
                stringBuilder.append(validationError.getMessage()).append("; ");
            }
            String errorString = stringBuilder.toString();
            throw new RuntimeException(errorString.substring(0, errorString.length() - 2));
        }
    }

    PackageRevision executeNuGetCmd(NuGetParams params) {
        return new NuGet(params).execute();
    }
}
