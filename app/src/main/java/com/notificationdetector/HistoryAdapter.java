package com.notificationdetector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private JSONArray data;
    private Context context;

    public HistoryAdapter(Context context, JSONArray data) {
        this.context = context;
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            JSONObject obj = data.getJSONObject(data.length() - 1 - position);
            String bank = obj.optString("bank", "");
            String title = obj.optString("title", "");
            String message = obj.optString("message", "");
            boolean isBank = obj.optBoolean("isBank", false);
            boolean sent = obj.optBoolean("sent", false);
            holder.titleText.setText(title);
            holder.messageText.setText(message);
            // Exibe status apenas se for notificação de banco, senão esconde
            if (isBank) {
                holder.statusText.setVisibility(View.VISIBLE);
                holder.statusText.setText(sent ? "Enviado" : "Falha");
                holder.statusText.setTextColor(sent ? 0xFF388E3C : 0xFFD32F2F); // verde ou vermelho
            } else {
                holder.statusText.setVisibility(View.GONE);
            }
            int iconRes = R.drawable.ic_bank_default;
            if (bank.toLowerCase().contains("c6"))
                iconRes = R.drawable.ic_bank_c6;
            // Adicione outros bancos aqui se desejar
            holder.bankIcon.setImageResource(iconRes);
        } catch (Exception e) {
            holder.titleText.setText("Erro");
            holder.messageText.setText("");
            holder.statusText.setVisibility(View.GONE);
            holder.bankIcon.setImageResource(R.drawable.ic_bank_default);
        }
    }

    @Override
    public int getItemCount() {
        return data.length();
    }

    public void updateData(JSONArray newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView bankIcon;
        TextView titleText, messageText, statusText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            bankIcon = itemView.findViewById(R.id.bankIcon);
            titleText = itemView.findViewById(R.id.titleText);
            messageText = itemView.findViewById(R.id.messageText);
            statusText = itemView.findViewById(R.id.statusText);
        }
    }
}
