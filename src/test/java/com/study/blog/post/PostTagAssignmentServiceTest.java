package com.study.blog.post;

import com.study.blog.tag.PostTagRepository;
import com.study.blog.tag.Tag;
import com.study.blog.tag.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostTagAssignmentServiceTest {

    @Mock
    private PostTagRepository postTagRepository;
    @Mock
    private TagRepository tagRepository;

    @Test
    void replaceTagsShouldFlushDeleteBeforeInsert() {
        PostTagAssignmentService service = new PostTagAssignmentService(postTagRepository, tagRepository);

        Post post = Post.builder().id(2L).build();
        Tag tag = Tag.builder().id(1L).name("spring").slug("spring").deletedYn("N").build();

        when(tagRepository.findByNameIgnoreCase("spring")).thenReturn(java.util.Optional.of(tag));
        when(tagRepository.save(tag)).thenReturn(tag);
        when(postTagRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.replaceTags(post, null, List.of("spring"));

        InOrder inOrder = inOrder(postTagRepository);
        inOrder.verify(postTagRepository).deleteByPost(post);
        inOrder.verify(postTagRepository).flush();
        inOrder.verify(postTagRepository).saveAllAndFlush(anyList());
    }
}
