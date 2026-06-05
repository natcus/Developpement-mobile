package com.enastic.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.enastic.chat.adapter.MemberAdapter;
import com.enastic.chat.model.Conversation;
import com.enastic.chat.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class GroupInfoActivity extends AppCompatActivity {

    private String conversationId;
    private FirebaseFirestore firestore;
    private String currentUserId;

    private ImageView imgGroup;
    private TextView textName, textCount;
    private RecyclerView recyclerView;
    private MemberAdapter adapter;
    private List<User> memberList;
    private Conversation currentConversation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        conversationId = getIntent().getStringExtra("conversationId");
        if (conversationId == null) {
            finish();
            return;
        }

        firestore = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        initViews();
        loadGroupInfo();
    }

    private void initViews() {
        imgGroup = findViewById(R.id.imgGroupLarge);
        textName = findViewById(R.id.textGroupNameInfo);
        textCount = findViewById(R.id.textMemberCount);
        recyclerView = findViewById(R.id.recyclerViewMembers);

        Toolbar toolbar = findViewById(R.id.toolbarGroupInfo);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        memberList = new ArrayList<>();
        adapter = new MemberAdapter(memberList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnMemberClickListener(new MemberAdapter.OnMemberClickListener() {
            @Override
            public void onMemberClick(User user) {
                // Optionnel : ouvrir le profil
            }

            @Override
            public void onMemberLongClick(User user) {
                if (currentConversation != null && currentConversation.getAdmins() != null && 
                    currentConversation.getAdmins().contains(currentUserId) && !user.getUid().equals(currentUserId)) {
                    showMemberOptionsDialog(user);
                }
            }
        });

        findViewById(R.id.btnAddMember).setOnClickListener(v -> showAddMemberDialog());
        findViewById(R.id.btnLeaveGroup).setOnClickListener(v -> leaveGroup());
    }

    private void loadGroupInfo() {
        firestore.collection("conversations").document(conversationId)
                .addSnapshotListener((value, error) -> {
                    if (value != null && value.exists()) {
                        Conversation convo = value.toObject(Conversation.class);
                        if (convo != null) {
                            currentConversation = convo;
                            textName.setText(convo.getName());
                            if (convo.getImageUrl() != null && !convo.getImageUrl().isEmpty()) {
                                Glide.with(this).load(convo.getImageUrl()).transform(new CircleCrop()).into(imgGroup);
                            }
                            loadMembers(convo.getMembers());
                        }
                    }
                });
    }

    private void loadMembers(List<String> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) return;

        firestore.collection("users").whereIn("uid", memberIds).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    memberList.clear();
                    memberList.addAll(queryDocumentSnapshots.toObjects(User.class));
                    adapter.notifyDataSetChanged();
                    textCount.setText(memberList.size() + " membres");
                });
    }

    private void showAddMemberDialog() {
        firestore.collection("users").get().addOnSuccessListener(querySnapshot -> {
            List<User> allUsers = querySnapshot.toObjects(User.class);
            List<User> availableUsers = new ArrayList<>();
            
            List<String> currentMemberIds = new ArrayList<>();
            for (User m : memberList) currentMemberIds.add(m.getUid());

            for (User u : allUsers) {
                if (!currentMemberIds.contains(u.getUid())) {
                    availableUsers.add(u);
                }
            }

            String[] names = new String[availableUsers.size()];
            for (int i = 0; i < availableUsers.size(); i++) names[i] = availableUsers.get(i).getName();

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Ajouter au groupe")
                    .setItems(names, (dialog, which) -> {
                        addMember(availableUsers.get(which).getUid());
                    })
                    .show();
        });
    }

    private void addMember(String userId) {
        firestore.collection("conversations").document(conversationId)
                .update("members", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Membre ajouté", Toast.LENGTH_SHORT).show());
    }

    private void leaveGroup() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Quitter")
                .setMessage("Voulez-vous vraiment quitter ce groupe ?")
                .setPositiveButton("Quitter", (dialog, which) -> {
                    firestore.collection("conversations").document(conversationId)
                            .update("members", FieldValue.arrayRemove(currentUserId))
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Vous avez quitté le groupe", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showMemberOptionsDialog(User user) {
        String[] options = {"Nommer Administrateur", "Retirer du groupe"};
        if (currentConversation.getAdmins().contains(user.getUid())) {
            options[0] = "Retirer les droits Admin";
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(user.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) toggleAdmin(user);
                    else removeMember(user.getUid());
                })
                .show();
    }

    private void toggleAdmin(User user) {
        boolean isAdmin = currentConversation.getAdmins().contains(user.getUid());
        if (isAdmin) {
            firestore.collection("conversations").document(conversationId)
                    .update("admins", FieldValue.arrayRemove(user.getUid()));
        } else {
            firestore.collection("conversations").document(conversationId)
                    .update("admins", FieldValue.arrayUnion(user.getUid()));
        }
    }

    private void removeMember(String userId) {
        firestore.collection("conversations").document(conversationId)
                .update("members", FieldValue.arrayRemove(userId), 
                        "admins", FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Membre retiré", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
