// Composant affichant l'espace de commentaires et d'echange sur un incident
import {
  List,
  Input,
  Typography,
  Avatar,
  Space,
  Divider,
  Empty,
  Upload,
  App,
  Button,
  Popconfirm,
} from "antd";
import type { UploadFile } from "antd";
import {
  PaperClipOutlined,
  SendOutlined,
  LoadingOutlined,
  CloseOutlined,
} from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { memo, useMemo, useCallback, useState, useRef } from "react";
import clsx from "clsx";
import {
  useIncidentComments,
  useAddComment,
  useUpdateComment,
  useDeleteComment,
} from "../../../../../hooks/incident/useIncidents/useIncidents";
import {
  useIncidentAttachments,
  useUploadCommentAttachment,
} from "../../../../../hooks/document/useDocuments";
import type { IncidentCommentResponse } from "../../../../../api/incident/types";
import type { AttachmentResponse } from "../../../../../api/document/types";
import {
  ACCEPTED_ATTACHMENT_ACCEPT,
  MAX_ATTACHMENT_SIZE_BYTES,
  isAcceptedAttachment,
  isAttachmentSizeValid,
} from "../../../../../utils/attachments/attachmentValidation";
import CommentAttachments from "../CommentAttachments";
import { formatDateTime } from "../../../../../utils/formatters/formatters";
import {
  actionIcons,
  formIcons,
  incidentIcons,
} from "../../../../../utils/icons/appIcons";
import styles from "./CommentsSection.module.scss";
import { useAuth } from "../../../../../hooks/auth/useAuth";

const { TextArea } = Input;
const { Text, Paragraph, Link } = Typography;

// Modele la structure flat comment node manipulee par le frontend.
interface FlatCommentNode extends IncidentCommentResponse {
  depth: number;
  hasChildren: boolean;
}

// Modele la structure comment tree node manipulee par le frontend.
interface CommentTreeNode extends IncidentCommentResponse {
  children: CommentTreeNode[];
}

// Construit comment tree pour l'affichage.
const buildCommentTree = (
  comments: IncidentCommentResponse[],
): FlatCommentNode[] => {
  const map = new Map<string, CommentTreeNode>();
  comments.forEach((c) => map.set(c.id, { ...c, children: [] }));

  const roots: CommentTreeNode[] = [];
  comments.forEach((c) => {
    if (c.parentCommentId && map.has(c.parentCommentId)) {
      map.get(c.parentCommentId)!.children.push(map.get(c.id)!);
    } else {
      roots.push(map.get(c.id)!);
    }
  });

  const flatten = (nodes: CommentTreeNode[], depth = 0): FlatCommentNode[] => {
    return nodes.reduce((acc: FlatCommentNode[], node: CommentTreeNode) => {
      const { children, ...rest } = node;
      acc.push({ ...rest, depth, hasChildren: children.length > 0 });
      acc.push(...flatten(children, depth + 1));
      return acc;
    }, []);
  };

  return flatten(roots);
};

// Definit les proprietes attendues par le composant CommentsSection.
interface CommentsSectionProps {
  incidentId: string;
  incidentStatus?: string;
}

// Statuts terminaux pour lesquels on bloque l'ajout de commentaires.
const COMMENTS_LOCKED_STATUSES = new Set<string>(["CLOSED", "REJECTED", "CANCELLED"]);

// Definit les proprietes attendues par le composant CommentItem.
interface CommentItemProps {
  comment: FlatCommentNode;
  canReply: boolean;
  canEdit: boolean;
  canDelete: boolean;
  onReply: (comment: IncidentCommentResponse) => void;
  onEditSave: (commentId: string, newContent: string) => void;
  onDelete: (commentId: string) => void;
  attachments: AttachmentResponse[];
}

// Rend le composant CommentItem pour l'interface comments section.
const CommentItem = memo(
  ({
    comment,
    canReply,
    canEdit,
    canDelete,
    onReply,
    onEditSave,
    onDelete,
    attachments,
  }: CommentItemProps) => {
    const { t } = useTranslation();
    const [isEditing, setIsEditing] = useState(false);
    const [editContent, setEditContent] = useState(comment.content);

    const authorFullName = useMemo(
      () => `${comment.author.firstName} ${comment.author.lastName}`,
      [comment.author.firstName, comment.author.lastName],
    );

    // Formate ted date pour l'interface.
    const formattedDate = useMemo(
      () => formatDateTime(comment.createdAt),
      [comment.createdAt],
    );

    const isNested = (comment.depth || 0) > 0;
    const avatarSize = isNested ? 24 : 32;
    const indentLeft = avatarSize + 8;

    const handleEditSave = () => {
      if (editContent.trim() && editContent !== comment.content) {
        onEditSave(comment.id, editContent);
      }
      setIsEditing(false);
    };

    const handleEditCancel = () => {
      setEditContent(comment.content);
      setIsEditing(false);
    };

    return (
      <List.Item
        className={clsx(styles.commentItem, {
          [styles.nestedComment]: isNested,
        })}
        style={{ "--depth": comment.depth || 0 } as React.CSSProperties}
      >
        <div className={styles.commentContent}>
          <div className={styles.commentHeader}>
            <Space size={8} align="center">
              <Avatar
                size={avatarSize}
                icon={formIcons.user}
                className={styles.avatar}
              />
              <div className={styles.authorInfo}>
                <Text strong className={styles.authorName}>
                  {authorFullName}
                </Text>
                <Text type="secondary" className={styles.commentDate}>
                  {formattedDate}
                </Text>
              </div>
            </Space>
          </div>
          {isEditing ? (
            <div style={{ paddingLeft: indentLeft, marginTop: 8 }}>
              <Input.TextArea
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                autoSize={{ minRows: 2, maxRows: 6 }}
                spellCheck={true}
              />
              <Space style={{ marginTop: 8 }}>
                <Button type="primary" size="small" onClick={handleEditSave}>
                  {t("common.save")}
                </Button>
                <Button size="small" onClick={handleEditCancel}>
                  {t("common.cancel")}
                </Button>
              </Space>
            </div>
          ) : (
            <>
              <Paragraph
                className={styles.commentBody}
                style={{ paddingLeft: indentLeft }}
              >
                {comment.content === "📎 Pièce(s) jointe(s)" && attachments.length === 0
                  ? t("incidents.attachments.missing_files")
                  : comment.content}
              </Paragraph>
              {attachments.length > 0 && (
                <div style={{ paddingLeft: indentLeft }}>
                  <CommentAttachments attachments={attachments} />
                </div>
              )}
              <Space size={16} style={{ marginLeft: indentLeft, marginTop: 8 }}>
                {canReply && (
                  <Link
                    className={styles.replyButton}
                    onClick={() => onReply(comment)}
                    aria-label={t("incidents.comments.reply")}
                  >
                    <Space size={4}>
                      {actionIcons.reply}
                      {t("incidents.comments.reply")}
                    </Space>
                  </Link>
                )}
                {canEdit && (
                  <Link
                    onClick={() => setIsEditing(true)}
                    aria-label={t("common.edit")}
                  >
                    <Space size={4}>
                      {actionIcons.edit}
                      {t("common.edit")}
                    </Space>
                  </Link>
                )}
                {canDelete && (
                  <Popconfirm
                    title={t("incidents.comments.delete_confirm")}
                    okText={t("common.yes")}
                    cancelText={t("common.no")}
                    okButtonProps={{ danger: true }}
                    onConfirm={() => onDelete(comment.id)}
                  >
                    <Link
                      aria-label={t("common.delete")}
                      className="ant-typography-danger"
                    >
                      <Space size={4}>
                        {actionIcons.delete}
                        {t("common.delete")}
                      </Space>
                    </Link>
                  </Popconfirm>
                )}
              </Space>
            </>
          )}
        </div>
      </List.Item>
    );
  },
);

// Rend le composant CommentsSection pour l'interface comments section.
const CommentsSection = memo(
  ({ incidentId, incidentStatus }: CommentsSectionProps) => {
    const { t } = useTranslation();
    const { message } = App.useApp();
    const { user } = useAuth();
    const [replyTo, setReplyTo] = useState<IncidentCommentResponse | null>(
      null,
    );
    const [content, setContent] = useState("");
    const [commentFiles, setCommentFiles] = useState<UploadFile[]>([]);
    const [pendingComment, setPendingComment] = useState<{
      id: string;
      hasAttachments: boolean;
      attachmentOnly: boolean;
    } | null>(null);
    const sendingRef = useRef(false);
    const [isSending, setIsSending] = useState(false);

    const { data: comments = [], isLoading } = useIncidentComments(incidentId);
    const { data: attachments = [] } = useIncidentAttachments(incidentId);
    const flatCommentsWithDepth = useMemo(
      () => buildCommentTree(comments),
      [comments],
    );

    // Regroupe les pieces jointes par commentaire.
    const attachmentsByComment = useMemo(() => {
      const grouped = new Map<string, AttachmentResponse[]>();
      attachments.forEach((attachment) => {
        if (!attachment.commentId) return;
        const list = grouped.get(attachment.commentId) ?? [];
        list.push(attachment);
        grouped.set(attachment.commentId, list);
      });
      return grouped;
    }, [attachments]);

    const { mutate: addComment, isPending } = useAddComment();
    const { mutate: updateComment } = useUpdateComment();
    const { mutate: deleteComment, mutateAsync: deleteEmptyComment } = useDeleteComment();
    const { mutateAsync: uploadCommentAttachment, isPending: isUploading } =
      useUploadCommentAttachment(incidentId);
    const canAddComments =
      !incidentStatus || !COMMENTS_LOCKED_STATUSES.has(incidentStatus);
    const isBusy = isSending || isPending || isUploading;

    // Publie le commentaire puis rattache les pieces jointes (via son commentId).
    const handleSend = useCallback(() => {
      if (sendingRef.current || isBusy || !canAddComments) return;
      const files = commentFiles
        .map((item) => item.originFileObj as File | undefined)
        .filter((file): file is File => Boolean(file));

      const invalidFile = files.find((file) => !isAttachmentSizeValid(file));
      if (invalidFile) {
        message.error(t(invalidFile.size === 0 ? "incidents.attachments.empty_file" : "incidents.attachments.too_large", {
          name: invalidFile.name,
          limit: MAX_ATTACHMENT_SIZE_BYTES / (1024 * 1024),
        }));
        return;
      }

      let trimmed = content.trim();
      if (!trimmed && files.length > 0) {
        trimmed = "📎 Pièce(s) jointe(s)";
      }

      if (!trimmed || isBusy) return;
      if (trimmed.length > 2000) {
        message.error(t("incidents.comments.form.validation.max"));
        return;
      }
      sendingRef.current = true;
      setIsSending(true);
      const finishSending = () => {
        sendingRef.current = false;
        setIsSending(false);
      };
      const sendFiles = async (created: Pick<IncidentCommentResponse, "id">) => {
        const failedFiles: UploadFile[] = [];
        let hasAttachments = pendingComment?.hasAttachments ?? false;
        const attachmentOnly = pendingComment?.attachmentOnly ?? !content.trim();
        try {
          for (const item of commentFiles) {
            if (!item.originFileObj) continue;
            try {
              await uploadCommentAttachment({ file: item.originFileObj, commentId: created.id });
              hasAttachments = true;
            } catch {
              failedFiles.push(item);
            }
          }
          setCommentFiles(failedFiles);
          setContent("");
          setReplyTo(null);
          if (failedFiles.length === 0) {
            setPendingComment(null);
            return;
          }
          setPendingComment({ id: created.id, hasAttachments, attachmentOnly });
          message.error(t("incidents.attachments.retry_files"));
          if (attachmentOnly && !hasAttachments) {
            try {
              await deleteEmptyComment({ id: incidentId, commentId: created.id, onlyIfEmpty: true });
              setPendingComment(null);
              setReplyTo(replyTo);
            } catch {
              // Conserve le commentaire cible si le nettoyage echoue.
            }
          }
        } finally {
          finishSending();
        }
      };
      if (pendingComment) {
        void sendFiles(pendingComment);
        return;
      }
      addComment(
        {
          id: incidentId,
          data: {
            content: trimmed,
            isInternal: false,
            parentCommentId: replyTo?.id,
          },
        },
        {
          onSuccess: sendFiles,
          onError: finishSending,
        },
      );
    }, [
      content,
      isBusy,
      canAddComments,
      pendingComment,
      deleteEmptyComment,
      commentFiles,
      addComment,
      incidentId,
      replyTo,
      uploadCommentAttachment,
      message,
      t,
    ]);

    const startReply = useCallback((comment: IncidentCommentResponse) => {
      setReplyTo(comment);
    }, []);

    const cancelReply = useCallback(() => setReplyTo(null), []);

    const handleEditSave = useCallback(
      (commentId: string, newContent: string) => {
        updateComment({
          id: incidentId,
          commentId,
          data: { content: newContent, isInternal: false },
        });
      },
      [incidentId, updateComment],
    );

    const handleDelete = useCallback(
      (commentId: string) => {
        deleteComment({ id: incidentId, commentId });
      },
      [incidentId, deleteComment],
    );

    const removeFile = useCallback((uid: string) => {
      if (sendingRef.current) return;
      if (commentFiles.length === 1) setPendingComment(null);
      setCommentFiles((prev) => prev.filter((file) => file.uid !== uid));
    }, [commentFiles.length]);

    const renderItem = useCallback(
      (comment: FlatCommentNode) => {
        const isAuthor = user?.id === comment.author.id;

        // Verifier s'il y a un commentaire posterieurement
        const commentDate = new Date(comment.createdAt).getTime();
        const hasSubsequentOtherUserComment = comments.some(
          (c) =>
            c.author.id !== user?.id &&
            new Date(c.createdAt).getTime() > commentDate,
        );

        const canEditOrDelete =
          isAuthor &&
          !comment.hasChildren &&
          !hasSubsequentOtherUserComment &&
          canAddComments &&
          !isBusy &&
          comment.id !== pendingComment?.id;

        return (
          <CommentItem
            key={comment.id}
            comment={comment}
            canReply={canAddComments && !isBusy && !pendingComment}
            canEdit={canEditOrDelete}
            canDelete={canEditOrDelete}
            onReply={startReply}
            onEditSave={handleEditSave}
            onDelete={handleDelete}
            attachments={attachmentsByComment.get(comment.id) ?? []}
          />
        );
      },
      [
        canAddComments,
        isBusy,
        pendingComment,
        startReply,
        attachmentsByComment,
        user,
        comments,
        handleEditSave,
        handleDelete,
      ],
    );

    // Valide un fichier (type, taille) avant de l'ajouter a la liste.
    const beforeUpload = useCallback(
      (file: File) => {
        if (!isAcceptedAttachment(file)) {
          message.error(t("incidents.attachments.invalid_type"));
          return Upload.LIST_IGNORE;
        }
        if (!isAttachmentSizeValid(file)) {
          message.error(
            t(file.size === 0 ? "incidents.attachments.empty_file" : "incidents.attachments.too_large", {
              name: file.name,
              limit: Math.round(MAX_ATTACHMENT_SIZE_BYTES / (1024 * 1024)),
            }),
          );
          return Upload.LIST_IGNORE;
        }
        return false;
      },
      [message, t],
    );

    // Collage d'image depuis le presse-papiers : ajoute directement en piece jointe.
    const handlePaste = useCallback(
      (event: React.ClipboardEvent<HTMLTextAreaElement>) => {
        if (isBusy) return;
        const items = event.clipboardData?.items;
        if (!items) return;
        const pasted: UploadFile[] = [];
        Array.from(items).forEach((item, index) => {
          if (item.kind !== "file" || !item.type.startsWith("image/")) return;
          const original = item.getAsFile();
          if (!original) return;
          const named =
            original.name && original.name !== "image.png"
              ? original
              : new File([original], `image-${Date.now()}-${index}.png`, {
                  type: original.type,
                });
          if (beforeUpload(named) === Upload.LIST_IGNORE) return;
          pasted.push({
            uid: `paste-${Date.now()}-${index}`,
            name: named.name,
            status: "done",
            originFileObj: named as unknown as UploadFile["originFileObj"],
          });
        });
        if (pasted.length > 0) {
          event.preventDefault();
          setCommentFiles((prev) => [...prev, ...pasted]);
        }
      },
      [beforeUpload, isBusy],
    );

    // Entree = envoyer ; Maj+Entree = nouvelle ligne (comportement messagerie).
    const handleEnter = useCallback(
      (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (event.key === "Enter" && !event.shiftKey) {
          event.preventDefault();
          if (isBusy) return;
          handleSend();
        }
      },
      [handleSend, isBusy],
    );

    return (
      <div className={styles.commentsSection}>
        <Divider titlePlacement="left" className={styles.sectionDivider}>
          <Space size={8}>
            {incidentIcons.comment}
            <Text strong>{t("incidents.comments.title")}</Text>
          </Space>
        </Divider>

        <List
          loading={isLoading}
          dataSource={flatCommentsWithDepth}
          renderItem={renderItem}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={t("incidents.comments.no_comments")}
              />
            ),
          }}
          className={styles.commentsList}
        />

        {canAddComments ? (
          <div className={styles.composer}>
            {pendingComment && (
              <Text type="warning">{t("incidents.attachments.retry_files")}</Text>
            )}
            {replyTo && (
              <div className={styles.replyBanner}>
                <Text className={styles.replyText}>
                  {t("incidents.comments.replying_to", {
                    name: `${replyTo.author.firstName} ${replyTo.author.lastName}`,
                  })}
                </Text>
                <button
                  type="button"
                  className={styles.iconMini}
                  onClick={cancelReply}
                  disabled={isBusy}
                  aria-label={t("common.cancel")}
                >
                  <CloseOutlined />
                </button>
              </div>
            )}

            {commentFiles.length > 0 && (
              <div className={styles.filePreview}>
                {commentFiles.map((file) => (
                  <span key={file.uid} className={styles.fileChip}>
                    {incidentIcons.attachment}
                    <span className={styles.fileName}>{file.name}</span>
                    <button
                      type="button"
                      className={styles.chipRemove}
                      onClick={() => removeFile(file.uid)}
                      disabled={isBusy}
                      aria-label={t("common.delete")}
                    >
                      <CloseOutlined />
                    </button>
                  </span>
                ))}
              </div>
            )}

            <div className={styles.inputBar}>
              <Upload
                showUploadList={false}
                multiple
                fileList={commentFiles}
                accept={ACCEPTED_ATTACHMENT_ACCEPT}
                beforeUpload={beforeUpload}
                onChange={({ fileList }) => setCommentFiles(fileList)}
                disabled={isBusy}
              >
                <button
                  type="button"
                  className={styles.iconButton}
                  aria-label={t("incidents.comments.form.attach")}
                  title={t("incidents.comments.form.attach")}
                  disabled={isBusy}
                >
                  <PaperClipOutlined />
                </button>
              </Upload>

              <TextArea
                className={styles.input}
                value={content}
                onChange={(event) => setContent(event.target.value)}
                onPaste={handlePaste}
                onKeyDown={handleEnter}
                placeholder={t("incidents.comments.form.placeholder")}
                maxLength={2000}
                autoSize={{ minRows: 1, maxRows: 6 }}
                variant="borderless"
                disabled={isBusy || Boolean(pendingComment)}
                spellCheck={true}
                aria-label={t("incidents.comments.form.label")}
              />

              <button
                type="button"
                className={styles.sendButton}
                onClick={handleSend}
                disabled={isBusy || (!content.trim() && commentFiles.length === 0)}
                aria-label={
                  isUploading
                    ? t("incidents.form.buttons.uploading")
                    : t("incidents.comments.form.submit")
                }
                title={
                  isUploading
                    ? t("incidents.form.buttons.uploading")
                    : t("incidents.comments.form.submit")
                }
              >
                {isBusy ? <LoadingOutlined spin /> : <SendOutlined />}
              </button>
            </div>
          </div>
        ) : (
          <Text type="secondary" className={styles.lockedHint}>
            {incidentStatus === "REJECTED"
              ? t("incidents.comments.locked_rejected")
              : t("incidents.comments.locked_closed")}
          </Text>
        )}
      </div>
    );
  },
);

export default CommentsSection;
