/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package cc.tweaked.processor;

import com.google.common.base.Strings;
import com.sun.source.doctree.*;

import javax.annotation.Nonnull;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Information about a class.
 */
public class ClassInfo
{
    private final String module;
    private final Kind kind;
    private final TypeElement element;
    private final DocCommentTree doc;

    private ClassInfo( @Nonnull String module, @Nonnull Kind kind, @Nonnull TypeElement element, @Nonnull DocCommentTree doc )
    {
        this.module = module;
        this.kind = kind;
        this.element = element;
        this.doc = doc;
    }

    /**
     * Attempt to construct a {@link ClassInfo}.
     *
     * @param env  The environment to construct within.
     * @param type The method we're wrapping.
     * @return Information about this method, if available.
     */
    @Nonnull
    public static Optional<ClassInfo> of( @Nonnull Environment env, @Nonnull TypeElement type )
    {
        DocCommentTree doc = env.trees().getDocCommentTree( type );
        if( doc == null ) return Optional.empty();

        Map<String, List<? extends DocTree>> tags = doc.getBlockTags().stream()
            .filter( UnknownBlockTagTree.class::isInstance ).map( UnknownBlockTagTree.class::cast )
            .filter( x -> x.getTagName().startsWith( "cc." ) )
            .collect( Collectors.toMap( BlockTagTree::getTagName, UnknownBlockTagTree::getContent ) );

        String name = getName( tags.get( "cc.module" ) );
        if( Strings.isNullOrEmpty( name ) ) return Optional.empty();

        Kind kind
            = env.types().isAssignable( type.asType(), env.getLuaApiType() ) ? Kind.API
            : env.types().isAssignable( type.asType(), env.getPeripheralType() ) ? Kind.PERIPHERAL
            : Kind.TYPE;

        return Optional.of( new ClassInfo( name, kind, type, doc ) );
    }

    private static String getName( List<? extends DocTree> tree )
    {
        if( tree == null ) return null;
        StringBuilder builder = new StringBuilder();
        for( DocTree child : tree )
        {
            if( child.getKind() == DocTree.Kind.TEXT ) builder.append( ((TextTree) child).getBody() );
        }
        return builder.toString();
    }

    public enum Kind
    {
        API,
        PERIPHERAL,
        TYPE
    }

    public String name()
    {
        return module;
    }

    public Kind kind()
    {
        return kind;
    }

    public TypeElement element()
    {
        return element;
    }

    public DocCommentTree doc()
    {
        return doc;
    }
}
