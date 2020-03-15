package com.example.animewatchlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

// http://www.ltm.fr/creez-une-vue-en-liste-listview-avec-texte-image/
public class animeListAdapter extends ArrayAdapter<String> {
    Context ctx;
    String[] imagesURLs;

    public animeListAdapter(Context context, String[] values, String[] URLs) {
        super(context, R.layout.anime_card, values);
        ctx = context;
        imagesURLs = URLs;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)
                getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.anime_card, parent, false);

        TextView textView = rowView.findViewById(R.id.animeTitleInList);
        ImageView imageView = rowView.findViewById(R.id.animeImageInList);

        textView.setText(getItem(position));

        if(convertView == null ){
            //imageView.setImageBitmap(decodeSampledBitmapFromResource(ctx.getResources(), R.drawable.fgo, 100, 100));
            //imageView.setImageResource(R.drawable.fgo);
            //String url = "https://cdn.myanimelist.net/images/anime/1429/95946.jpg?s=54a1d4bcd881957ce164297f36df5a72";
            Picasso.with(ctx)
                    .load(imagesURLs[position])
                    .resize(200,200)
                    .into(imageView);
        }
        else
            rowView = (View)convertView.getTag();
            // https://stackoverflow.com/questions/28566373/android-listview-showing-only-first-4-items-on-repeat

        return rowView;
    }
}
