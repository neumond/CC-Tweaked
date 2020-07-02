/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package cc.tweaked.processor;

import com.sun.source.doctree.*;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SimpleDocTreeVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocConverter extends SimpleDocTreeVisitor<Void, StringBuilder>
{
    private final Element owner;
    private final Environment environment;

    private boolean hasParam = false;
    private final Map<String, List<? extends DocTree>> params = new HashMap<>();

    private boolean hasReturn = false;
    private List<? extends DocTree> returns;

    public static String of( Environment env, Element owner, DocTree tree )
    {
        return new DocConverter( env, owner ).visit( tree, new StringBuilder() ).toString();
    }

    public static String of( Environment env, Element owner, Iterable<? extends DocTree> tree )
    {
        return new DocConverter( env, owner ).visit( tree, new StringBuilder() ).toString();
    }

    public DocConverter( Environment environment, Element owner )
    {
        this.owner = owner;
        this.environment = environment;
    }

    public boolean hasParam()
    {
        return hasParam;
    }

    @Nonnull
    public Map<String, List<? extends DocTree>> getParams()
    {
        return params;
    }

    public boolean hasReturn()
    {
        return hasReturn;
    }

    @Nullable
    public List<? extends DocTree> getReturns()
    {
        return returns;
    }

    @Override
    public Void visitDocComment( DocCommentTree node, StringBuilder stringBuilder )
    {
        visit( node.getFullBody(), stringBuilder );
        stringBuilder.append( "\n" );
        visit( node.getBlockTags(), stringBuilder );
        return null;
    }

    @Override
    public Void visitText( TextTree node, StringBuilder stringBuilder )
    {
        stringBuilder.append( node.getBody() );
        return null;
    }

    @Override
    public Void visitLiteral( LiteralTree node, StringBuilder stringBuilder )
    {
        String body = node.getBody().getBody();
        switch( body )
        {
            case "nil":
            case "true":
            case "false":
                stringBuilder.append( "@{" ).append( body ).append( "}" );
                return null;
            default:
                stringBuilder.append( "`" ).append( body ).append( "`" );
                return null;
        }
    }

    @Override
    public Void visitLink( LinkTree node, StringBuilder stringBuilder )
    {
        stringBuilder.append( "@{" );
        visit( node.getReference(), stringBuilder );
        if( !node.getLabel().isEmpty() )
        {
            stringBuilder.append( "|" );
            visit( node.getLabel(), stringBuilder );
        }
        stringBuilder.append( "}" );
        return null;
    }

    @Override
    public Void visitReference( ReferenceTree node, StringBuilder stringBuilder )
    {
        // TODO: Resolve and map correctly!
        stringBuilder.append( node.getSignature() );
        return null;
    }

    @Override
    public Void visitHidden( HiddenTree node, StringBuilder stringBuilder )
    {
        stringBuilder.append( "@local\n" );
        return null;
    }

    @Override
    public Void visitParam( ParamTree node, StringBuilder stringBuilder )
    {
        params.put( node.getName().getName().toString(), node.getDescription() );
        return null;
    }

    @Override
    public Void visitReturn( ReturnTree node, StringBuilder stringBuilder )
    {
        returns = node.getDescription();
        return null;
    }

    @Override
    public Void visitThrows( ThrowsTree node, StringBuilder stringBuilder )
    {
        List<? extends DocTree> desc = node.getDescription();
        if( !desc.isEmpty() && desc.get( 0 ).getKind() == DocTree.Kind.TEXT && ((TextTree) desc.get( 0 )).getBody().startsWith( "(hidden)" ) )
        {
            return null;
        }

        stringBuilder.append( "@throws " );
        visit( desc, stringBuilder );
        stringBuilder.append( "\n" );
        return null;
    }

    @Override
    public Void visitDeprecated( DeprecatedTree node, StringBuilder stringBuilder )
    {
        stringBuilder.append( "@deprecated " );
        visit( node.getBody(), stringBuilder );
        stringBuilder.append( "\n" );
        return null;
    }

    @Override
    public Void visitUnknownBlockTag( UnknownBlockTagTree node, StringBuilder stringBuilder )
    {
        String name = node.getTagName();
        if( !name.startsWith( "cc." ) ) return super.visitUnknownBlockTag( node, stringBuilder );

        String actualName = name.substring( 3 );
        switch( actualName )
        {
            case "param":
            case "tparam":
                hasParam = true;
                break;
            case "return":
            case "treturn":
                hasReturn = true;
                break;
            case "module":
                return null;
        }

        stringBuilder.append( "@" ).append( actualName ).append( " " );
        visit( node.getContent(), stringBuilder );
        stringBuilder.append( "\n" );
        return null;
    }

    @Override
    public Void visitSee( SeeTree node, StringBuilder stringBuilder )
    {
        stringBuilder.append( "@see " );
        visit( node.getReference(), stringBuilder );
        stringBuilder.append( "\n" );
        return null;
    }

    @Override
    public Void visitStartElement( StartElementTree node, StringBuilder stringBuilder )
    {
        if( node.getName().contentEquals( "pre" ) )
        {
            stringBuilder.append( "```lua" );
            return null;
        }

        return super.visitStartElement( node, stringBuilder );
    }

    @Override
    public Void visitEndElement( EndElementTree node, StringBuilder stringBuilder )
    {
        if( node.getName().contentEquals( "pre" ) )
        {
            stringBuilder.append( "```" );
            return null;
        }
        return super.visitEndElement( node, stringBuilder );
    }

    @Override
    protected Void defaultAction( DocTree node, StringBuilder stringBuilder )
    {
        report( node, "Visiting unknown node " + node.getKind() );
        return null;
    }

    protected void report( DocTree node, String message )
    {
        DocTrees trees = environment.trees();
        environment.trees().printMessage(
            Diagnostic.Kind.ERROR, message, node,
            trees.getDocCommentTree( owner ), trees.getPath( owner ).getCompilationUnit()
        );
    }
}
