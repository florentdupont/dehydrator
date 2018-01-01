package dehydrator;

import lombok.val;

import javax.lang.model.element.AnnotationValue;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class DehydrateContext {

    private DehydrateContext() {}

    String suffix;

    String name;

    String excludedCollectionClass;

    String parentEntityClass;

    List<String> of;

    List<String> exclude;

    String targetPackage;

    static DehydrateContext fromMap(Map<String, Object> values) {
        val context = new DehydrateContext();
        context.suffix = (String) values.get("suffix");
        context.name = (String) values.get("name");
        context.excludedCollectionClass = (String) values.get("excludedCollectionClass");
        context.parentEntityClass = (String) values.get("parentEntityClass");
        context.of = ((List<AnnotationValue>)values.get("of")).stream().map(x->(String)x.getValue()).collect(Collectors.toList());
        context.exclude = ((List<AnnotationValue>)values.get("exclude")).stream().map(x->(String)x.getValue()).collect(Collectors.toList());
        context.targetPackage = (String) values.get("targetPackage");
        return context;
    }


}
