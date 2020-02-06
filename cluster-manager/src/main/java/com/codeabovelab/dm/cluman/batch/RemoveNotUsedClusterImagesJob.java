

package com.codeabovelab.dm.cluman.batch;

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.RemoveImageArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.RemoveImageResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.ImageItem;
import com.codeabovelab.dm.cluman.cluster.filter.FilterFactory;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryRepository;
import com.codeabovelab.dm.cluman.job.JobBean;
import com.codeabovelab.dm.cluman.job.JobContext;
import com.codeabovelab.dm.cluman.job.JobParam;
import com.codeabovelab.dm.cluman.model.DiscoveryStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetImagesArg.ALL;
import static com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode.OK;

/**
 * Clear all not used images from cluster
 */
@JobBean("job.removeClusterImages")
public class RemoveNotUsedClusterImagesJob implements Runnable {

    @JobParam(required = true)
    private String clusterName;

    @Autowired
    private JobContext context;

    @Autowired
    private RegistryRepository registryRepository;

    @Autowired
    private DiscoveryStorage dockerServices;

    @Autowired
    private FilterFactory filterFactory;

    @Override
    public void run() {
        context.fire("About to delete all not used images from: \"{0}\".", clusterName);

        DockerService service = dockerServices.getService(clusterName);
        List<ImageItem> images = service.getImages(ALL);

        if (CollectionUtils.isEmpty(images)) {
            context.fire("Nothing to remove, skipping");
            return;
        }
        List<RemoveImageResult> result = images.stream().map(i -> service
                .removeImage(RemoveImageArg.builder()
                .cluster(clusterName)
                .imageId(i.getId())
                .build())).collect(Collectors.toList());

        context.fire("Were deleted: \"{0}\", next images are used: \"{1}\"",
                filter(result, r -> r.getCode() == OK),
                filter(result, r -> r.getCode() != OK));
    }

    private List<String> filter(List<RemoveImageResult> result, Predicate<RemoveImageResult> filter) {
        return result.stream().filter(filter).map(RemoveImageResult::getImage).collect(Collectors.toList());
    }

}

