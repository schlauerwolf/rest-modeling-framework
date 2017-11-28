package io.vrap.rmf.raml.generic.generator.postman;

import io.vrap.rmf.raml.generic.generator.Generator;
import io.vrap.rmf.raml.generic.generator.Helper;
import io.vrap.rmf.raml.model.modules.Api;
import io.vrap.rmf.raml.model.util.StringCaseFormat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class PostmanGenerator implements Generator {
    private final String vendorName;

    public PostmanGenerator(final String vendorName) {
        this.vendorName = vendorName;
    }

    @Override
    public void generate(final Api api, final File outputPath) throws IOException {
        final String title = StringCaseFormat.UPPER_CAMEL_CASE.apply(api.getTitle());
        final String vendorName = Optional.ofNullable(this.vendorName).orElse(title);

        Helper.ensureDirectory(outputPath);

        final CollectionGenerator generator = new CollectionGenerator(vendorName);
        final List<File> f = generator.generate(outputPath, api);

        Helper.deleteObsoleteFiles(outputPath, f);
    }
}
