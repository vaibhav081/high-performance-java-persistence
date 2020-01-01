package com.vladmihalcea.book.hpjp.hibernate.mapping.softdelete;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.junit.Test;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.MapsId;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.Table;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
public class SoftDeleteTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostDetails.class,
                PostComment.class,
                Tag.class
        };
    }

    @Override
    public void init() {
        super.init();

        doInJPA(entityManager -> {
            Tag javaTag = new Tag();
            javaTag.setId("Java");
            entityManager.persist(javaTag);

            Tag jpaTag = new Tag();
            jpaTag.setId("JPA");
            entityManager.persist(jpaTag);

            Tag hibernateTag = new Tag();
            hibernateTag.setId("Hibernate");
            entityManager.persist(hibernateTag);

            Tag miscTag = new Tag();
            miscTag.setId("Misc");
            entityManager.persist(miscTag);
        });
    }

    @Test
    public void testRemoveTag() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");

            entityManager.persist(post);

            post.addTag(entityManager.getReference(Tag.class, "Java"));
            post.addTag(entityManager.getReference(Tag.class, "Hibernate"));
            post.addTag(entityManager.getReference(Tag.class, "Misc"));
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(3, post.getTags().size());
        });

        doInJPA(entityManager -> {
            Tag miscTag = entityManager.getReference(Tag.class, "Misc");
            entityManager.remove(miscTag);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(2, post.getTags().size());
        });

        doInJPA(entityManager -> {
            //That would not work without @Loader(namedQuery = "findTagById")
            assertNull(entityManager.find(Tag.class, "Misc"));
        });

        doInJPA(entityManager -> {
            List<Tag> tags = entityManager.createQuery("select t from Tag t", Tag.class).getResultList();
            //That would not work without @Where(clause = "deleted = false")
            assertEquals(3, tags.size());
        });
    }

    @Test
    public void testRemovePostDetails() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");

            PostDetails postDetails = new PostDetails();
            postDetails.setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2016, 11, 2, 12, 0, 0)));
            post.addDetails(postDetails);

            entityManager.persist(post);

            post.addTag(entityManager.getReference(Tag.class, "Java"));
            post.addTag(entityManager.getReference(Tag.class, "Hibernate"));
            post.addTag(entityManager.getReference(Tag.class, "Misc"));

            PostComment comment1 = new PostComment();
            comment1.setId(1L);
            comment1.setReview("Great!");
            post.addComment(comment1);

            PostComment comment2 = new PostComment();
            comment2.setId(2L);
            comment2.setReview("To read");
            post.addComment(comment2);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertNotNull(post.getDetails());
            post.removeDetails();
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertNull(post.getDetails());
        });

        doInJPA(entityManager -> {
            assertNull(entityManager.find(PostDetails.class, 1L));
        });
    }

    @Test
    public void testRemovePostComment() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");

            PostDetails postDetails = new PostDetails();
            postDetails.setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2016, 11, 2, 12, 0, 0)));
            post.addDetails(postDetails);

            entityManager.persist(post);

            post.addTag(entityManager.getReference(Tag.class, "Java"));
            post.addTag(entityManager.getReference(Tag.class, "Hibernate"));
            post.addTag(entityManager.getReference(Tag.class, "Misc"));

            PostComment comment1 = new PostComment();
            comment1.setId(1L);
            comment1.setReview("Great!");
            post.addComment(comment1);

            PostComment comment2 = new PostComment();
            comment2.setId(2L);
            comment2.setReview("To read");
            post.addComment(comment2);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(2, post.getComments().size());
            assertNotNull(entityManager.find(PostComment.class, 2L));
            post.removeComment(post.getComments().get(1));
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(1, post.getComments().size());
            assertNull(entityManager.find(PostComment.class, 2L));
        });
    }

    @Test
    public void testRemoveAndFindPostComment() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);

            PostComment comment1 = new PostComment();
            comment1.setId(1L);
            comment1.setReview("Great!");
            post.addComment(comment1);

            PostComment comment2 = new PostComment();
            comment2.setId(2L);
            comment2.setReview("Excellent!");
            post.addComment(comment2);
        });
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            post.removeComment(post.getComments().get(0));
        });
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(1, post.getComments().size());
        });
    }

    /**
     * Purpose: (post_id, tag_id) Record should not be deleted from post_tag table after soft deleting post entity.
     */
    @Test
    public void testRemovePost_checkIsSoftDeleted_thenFetchTags() {

        // Creating post and tags
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("Many-to-Many association table size test");
            entityManager.persist(post);

            post.addTag(entityManager.getReference(Tag.class, "Java"));
            post.addTag(entityManager.getReference(Tag.class, "Hibernate"));
            post.addTag(entityManager.getReference(Tag.class, "Misc"));
        });
        // Fetching and asserting tags count for this post, then soft deleting post entity object.
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(post.getTags().size(), 3);
            entityManager.remove(post);
        });
        // Verifying post entity object with id = 1 is soft deleted.
        doInJPA(entityManager -> {
            Query query = entityManager.createNativeQuery("SELECT p.id, p.deleted FROM post p where p.id = ?");
            query.setParameter(1, 1);
            Object[] post = (Object[]) query.getSingleResult();
            assertEquals(java.util.Optional.of(BigInteger.valueOf(1L)).get(), post[0]);
            assertEquals(true, post[1]);
        });

        // Though no soft or hard delete is happening on tags.
        doInJPA(entityManager -> {
            assertEquals("Java", entityManager.find(Tag.class, "Java").getId());
            assertEquals("Hibernate", entityManager.find(Tag.class, "Hibernate").getId());
            assertEquals("Misc", entityManager.find(Tag.class, "Misc").getId());

            Query query = entityManager.createNativeQuery("SELECT COUNT(*) FROM post_tag pt where pt.post_id = ?");
            query.setParameter(1, 1);
            assertEquals("total tags must be same", BigInteger.valueOf(3L), query.getSingleResult());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @SQLDelete(sql =
            "UPDATE post " +
                    "SET deleted = true " +
                    "WHERE id = ?")
    @Loader(namedQuery = "findPostById")
    @NamedQuery(name = "findPostById", query =
            "select p " +
                    "from Post p " +
                    "where " +
                    "	p.id = ?1 and " +
                    "	p.deleted = false")
    @Where(clause = "deleted = false")
    public static class Post extends BaseEntity {

        @Id
        private Long id;

        private String title;

        @OneToMany(
                mappedBy = "post",
                cascade = CascadeType.ALL,
                orphanRemoval = true
        )
        private List<PostComment> comments = new ArrayList<>();

        @OneToOne(
                mappedBy = "post",
                cascade = CascadeType.ALL,
                orphanRemoval = true,
                fetch = FetchType.LAZY
        )
        private PostDetails details;

        @ManyToMany
        @JoinTable(
                name = "post_tag",
                joinColumns = @JoinColumn(name = "post_id"),
                inverseJoinColumns = @JoinColumn(name = "tag_id")
        )
        private List<Tag> tags = new ArrayList<>();

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<PostComment> getComments() {
            return comments;
        }

        public PostDetails getDetails() {
            return details;
        }

        public List<Tag> getTags() {
            return tags;
        }

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }

        public void removeComment(PostComment comment) {
            comments.remove(comment);
            comment.setPost(null);
        }

        public void addDetails(PostDetails details) {
            this.details = details;
            details.setPost(this);
        }

        public void removeDetails() {
            details.setPost(null);
            details = null;
        }

        public void addTag(Tag tag) {
            tags.add(tag);
        }
    }

    @Entity(name = "PostDetails")
    @Table(name = "post_details")
    @SQLDelete(sql =
            "UPDATE post_details " +
                    "SET deleted = true " +
                    "WHERE id = ?")
    @Loader(namedQuery = "findPostDetailsById")
    @NamedQuery(name = "findPostDetailsById", query =
            "select pd " +
                    "from PostDetails pd " +
                    "where " +
                    "	pd.id = ?1 and " +
                    "	pd.deleted = false")
    @Where(clause = "deleted = false")
    public static class PostDetails extends BaseEntity {

        @Id
        private Long id;

        @Column(name = "created_on")
        private Date createdOn;

        @Column(name = "created_by")
        private String createdBy;
        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "id")
        @MapsId
        private Post post;

        public PostDetails() {
            createdOn = new Date();
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    @SQLDelete(sql =
            "UPDATE post_comment " +
                    "SET deleted = true " +
                    "WHERE id = ?")
    @Loader(namedQuery = "findPostCommentById")
    @NamedQuery(name = "findPostCommentById", query =
            "select pc " +
                    "from PostComment pc " +
                    "where " +
                    "	pc.id = ?1 and " +
                    "	pc.deleted = false")
    @Where(clause = "deleted = false")
    public static class PostComment extends BaseEntity {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }
    }

    @Entity(name = "Tag")
    @Table(name = "tag")
    @SQLDelete(sql =
            "UPDATE tag " +
                    "SET deleted = true " +
                    "WHERE id = ?")
    @Loader(namedQuery = "findTagById")
    @NamedQuery(name = "findTagById", query =
            "select t " +
                    "from Tag t " +
                    "where " +
                    "	t.id = ?1 and " +
                    "	t.deleted = false")
    @Where(clause = "deleted = false")
    public static class Tag extends BaseEntity {

        @Id
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    @MappedSuperclass
    public static abstract class BaseEntity {

        private boolean deleted;
    }
}
