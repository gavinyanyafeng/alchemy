package com.dfire.platform.alchemy.web.descriptor;

import com.dfire.platform.alchemy.web.common.Constants;

/**
 * @author congbai
 * @date 2018/6/8
 */
public class JarInfoDescriptor implements Descriptor {

    private String jarPath;

    /**
     * 为防止本地文件丢失，将jar包上传到远程文件服务器
     */
    private String remoteUrl;

    private Integer parallelism;

    private String[] programArgs;

    private String entryClass;

    public String getJarPath() {
        return jarPath;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public Integer getParallelism() {
        return parallelism;
    }

    public void setParallelism(Integer parallelism) {
        this.parallelism = parallelism;
    }

    public String[] getProgramArgs() {
        return programArgs;
    }

    public void setProgramArgs(String[] programArgs) {
        this.programArgs = programArgs;
    }

    public String getEntryClass() {
        return entryClass;
    }

    public void setEntryClass(String entryClass) {
        this.entryClass = entryClass;
    }

    @Override
    public String getType() {
        return Constants.TYPE_VALUE_JAR;
    }

    @Override
    public void validate() throws Exception {
        //// TODO: 2018/6/8
    }
}