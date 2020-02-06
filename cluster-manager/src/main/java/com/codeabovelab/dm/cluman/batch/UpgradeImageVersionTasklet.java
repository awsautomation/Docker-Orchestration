

package com.codeabovelab.dm.cluman.batch;

import com.codeabovelab.dm.cluman.cluster.registry.RegistryRepository;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryService;
import com.codeabovelab.dm.cluman.cluster.registry.data.Tags;
import com.codeabovelab.dm.cluman.job.JobComponent;
import com.codeabovelab.dm.cluman.job.JobContext;
import com.codeabovelab.dm.cluman.job.JobParam;
import com.codeabovelab.dm.cluman.model.ImageName;
import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.List;

import static com.codeabovelab.dm.cluman.batch.LoadContainersOfImageTasklet.JP_IMAGE;

/**
 * Upgrade version of container
 */
@JobComponent
@Slf4j
public class UpgradeImageVersionTasklet {

    @JobParam(value = JP_IMAGE, required = true)
    private ImagesForUpdate images;

    @Autowired
    private RegistryRepository registries;

    @Autowired
    private JobContext context;

    public ProcessedContainer execute(ProcessedContainer item) {
        String version = resolveVersion(item);
        final String image = item.getImage();
        final String newImage = ImageName.setTag(image, version);
        log.info("updating to {}", newImage);
        item = item.makeNew()
          .image(newImage)
          .imageId(null) // we also need to reset image id
          .build();
        context.fire("Upgrade version of container \"{0}\" from \"{1}\" to \"{2}\"",
                item.getName(),
                ContainerUtils.getImageVersion(image),
                version);
        return item;
    }

    private String resolveVersion(ProcessedContainer item) {
        final String fullImageName = item.getImage();
        ImagesForUpdate.Image imgRecord = images.findImage(fullImageName, item.getImageId());
        Assert.notNull(imgRecord, "can not find image record for " + item);
        String to = imgRecord.getTo();
        if(to != null && !ImagesForUpdate.isPattern(to)) {
            return to;
        }
        Assert.isTrue(!ImageName.isId(fullImageName), item + " can not be updated because " + imgRecord +
          " does not specify concrete version, and contained has only id, we can not load tag list for it.");
        String imageName = ImageName.withoutTag(fullImageName);
        RegistryService registry = registries.getRegistryByImageName(imageName);
        Assert.notNull(registry, "Can not find registry for " + imageName);
        Tags tags = registry.getTags(imageName);
        Assert.notNull(tags, "Can not load tags for " + imageName);
        List<String> tagList = tags.getTags();
        if(tagList.isEmpty()) {
            // i am not sure that it error, may in some cases image without tags is correct, but now we check it
            throw new RuntimeException("Image " + imageName + " does not has any tags.");
        }
        return tagList.get(tagList.size() - 1);
    }
}
