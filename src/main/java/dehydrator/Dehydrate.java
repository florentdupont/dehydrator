package dehydrator;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(Dehydrates.class)
public @interface Dehydrate {


    /**
     * Suffix of the generated class name.
     *
     * Will be ignored if name is already set.
     */
    String suffix() default "Dehydrated";

    /**
     * If present, the name of the generated class.
     * If not specified, the generated class will have the original class name prefixed by the 'prefix" value.
     */
    String name() default "";

    /**
     * Class for common collections representing x-to-Many associations to other Entities.
     */
    String excludedCollectionClass() default "java.lang.Iterable";


    /**
     * Parent class representing the Entity
     */
    String parentEntityClass() default "com.example.demo.domain.AbstractEntity";

    /**
     * If present, explicitly lists the fields that are to be included.
     * Normally,
     * - static fields are excluded.
     * - fields implementing Iterable are excluded
     * - serialVersionUID field is excluded
     * <p>
     * Mutually exclusive with {@link #exclude()}.
     *
     * @return A list of fields to use (<em>default</em>: all of them).
     */
    String[] of() default {};

    /**
     * Any fields listed here will not be printed in the generated {@code toString} implementation.
     * Mutually exclusive with {@link #of()}.
     *
     * @return A list of fields to exclude.
     */
    String[] exclude() default {};

    /**
     *
     * Name of the package to generate to.
     *
     * If not specified, will be generated with the same package name.
     */
    String targetPackage() default "";

}