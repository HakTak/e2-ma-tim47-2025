package com.example.projekatmobilne.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.enums.EquipmentSubtype;
import com.example.projekatmobilne.enums.EquipmentType;
import com.example.projekatmobilne.models.Equipment;

import java.util.List;

public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.EquipmentViewHolder> {

    public interface OnEquipmentActionListener {
        void onActivate(Equipment equipment);
        void onUpgrade(Equipment equipment);
    }

    private final Context context;
    private List<Equipment> equipmentList;
    private final OnEquipmentActionListener listener;
    private final int userLevel;
    private final int userCoins;

    public EquipmentAdapter(Context context, List<Equipment> equipmentList,
                            OnEquipmentActionListener listener,
                            int userLevel, int userCoins) {
        this.context = context;
        this.equipmentList = equipmentList;
        this.listener = listener;
        this.userLevel = userLevel;
        this.userCoins = userCoins;
    }

    @NonNull
    @Override
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_equipment, parent, false);
        return new EquipmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipmentViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);

        // Ikonica
        holder.tvIcon.setText(getIcon(equipment.getSubtype()));

        // Naziv
        holder.tvName.setText(equipment.getDisplayName());

        // Bonus opis
        holder.tvBonus.setText(getBonusDescription(equipment));

        // Status
        if (equipment.isActive()) {
            holder.tvStatus.setText("✅ Aktivna");
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
        } else if (equipment.isUsed()) {
            holder.tvStatus.setText("❌ Potrošena");
            holder.tvStatus.setTextColor(Color.parseColor("#B71C1C"));
        } else if (equipment.getType() == EquipmentType.CLOTHING && equipment.getFightsRemaining() > 0) {
            holder.tvStatus.setText("🛡️ Preostalo borbi: " + equipment.getFightsRemaining());
            holder.tvStatus.setTextColor(Color.parseColor("#1565C0"));
        } else {
            holder.tvStatus.setText("💤 Neaktivna");
            holder.tvStatus.setTextColor(Color.parseColor("#757575"));
        }

        // Dugme akcije
        if (equipment.getType() == EquipmentType.WEAPON) {
            holder.btnAction.setText("Unapredi");
            holder.btnAction.setEnabled(true);
            holder.btnAction.setOnClickListener(v -> listener.onUpgrade(equipment));
        } else if (equipment.isActive() || equipment.isUsed()) {
            holder.btnAction.setText("Aktivna");
            holder.btnAction.setEnabled(false);
        } else {
            holder.btnAction.setText("Aktiviraj");
            holder.btnAction.setEnabled(true);
            holder.btnAction.setOnClickListener(v -> listener.onActivate(equipment));
        }
    }

    @Override
    public int getItemCount() {
        return equipmentList != null ? equipmentList.size() : 0;
    }

    public void updateList(List<Equipment> newList) {
        this.equipmentList = newList;
        notifyDataSetChanged();
    }

    // ===== HELPER =====

    private String getIcon(EquipmentSubtype subtype) {
        switch (subtype) {
            case POTION_20:
            case POTION_40:       return "🧪";
            case POTION_PERM_5:
            case POTION_PERM_10:  return "⚗️";
            case GLOVES:          return "🥊";
            case SHIELD:          return "🛡️";
            case BOOTS:           return "👢";
            case SWORD:           return "⚔️";
            case BOW:             return "🏹";
            default:              return "❓";
        }
    }

    private String getBonusDescription(Equipment equipment) {
        switch (equipment.getSubtype()) {
            case POTION_20:      return "+20% PP (jednokratno)";
            case POTION_40:      return "+40% PP (jednokratno)";
            case POTION_PERM_5:  return "+5% PP (trajno)";
            case POTION_PERM_10: return "+10% PP (trajno)";
            case GLOVES:         return "+10% PP • 2 borbe";
            case SHIELD:         return "+10% šansa napada • 2 borbe";
            case BOOTS:          return "40% šansa za +1 napad • 2 borbe";
            case SWORD:
                return String.format("+%.2f%% PP (trajno)", equipment.getWeaponBonus() * 100);
            case BOW:
                return String.format("+%.2f%% novčići (trajno)", equipment.getWeaponBonus() * 100);
            default: return "";
        }
    }

    // ===== VIEW HOLDER =====
    public static class EquipmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvName, tvBonus, tvStatus;
        Button btnAction;

        public EquipmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon   = itemView.findViewById(R.id.tvEquipmentIcon);
            tvName   = itemView.findViewById(R.id.tvEquipmentName);
            tvBonus  = itemView.findViewById(R.id.tvEquipmentBonus);
            tvStatus = itemView.findViewById(R.id.tvEquipmentStatus);
            btnAction = itemView.findViewById(R.id.btnEquipmentAction);
        }
    }
}